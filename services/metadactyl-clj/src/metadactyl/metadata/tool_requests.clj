(ns metadactyl.metadata.tool-requests
  (:use [clojure.java.io :only [reader]]
        [kameleon.entities]
        [korma.core :exclude [update]]
        [korma.db]
        [metadactyl.user :only [load-user]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.string :as string]
            [kameleon.queries :as queries]
            [metadactyl.clients.notifications :as cn]
            [metadactyl.util.params :as params])
  (:import [java.util UUID]))

;; Status codes.
(def ^:private initial-status-code "Submitted")

(defn- required-field
  "Extracts a required field from a map."
  [m & ks]
  (let [v (first (remove string/blank? (map m ks)))]
    (when (nil? v)
      (throw+ {:type :clojure-commons.exception/missing-request-field
               :accepted_keys ks}))
    v))

(defn- architecture-name-to-id
  "Gets the internal architecture identifier for an architecture name."
  [architecture]
  (let [id (:id (first (select tool_architectures (where {:name architecture}))))]
    (when (nil? id)
      (throw+ {:type  :clojure-commons.exception/not-found
               :error (str "Could not locate ID for the architecture name: " architecture)}))
    id))

(defn- status-code-subselect
  "Creates a subselect statement to find the primary key of a status code."
  [status-code]
  (subselect tool_request_status_codes
             (fields :id)
             (where {:name status-code})))

(defn- handle-new-tool-request
  "Submits a tool request on behalf of the authenticated user."
  [username req]
  (transaction
   (let [user-id         (queries/get-user-id username)
         architecture-id (architecture-name-to-id (required-field req :architecture))
         uuid            (UUID/randomUUID)]

     (insert tool_requests
             (values {:phone                (:phone req)
                      :id                   uuid
                      :tool_name            (required-field req :name)
                      :description          (required-field req :description)
                      :source_url           (required-field req :source_url :source_upload_file)
                      :doc_url              (required-field req :documentation_url)
                      :version              (required-field req :version)
                      :attribution          (:attribution req "")
                      :multithreaded        (:multithreaded req)
                      :test_data_path       (required-field req :test_data_path)
                      :instructions         (required-field req :cmd_line)
                      :additional_info      (:additional_info req)
                      :additional_data_file (:additional_data_file req)
                      :requestor_id         user-id
                      :tool_architecture_id architecture-id}))

     (insert tool_request_statuses
             (values {:tool_request_id             uuid
                      :tool_request_status_code_id (status-code-subselect initial-status-code)
                      :updater_id                  user-id}))
     uuid)))

(defn- get-tool-req
  "Loads a tool request from the database."
  [uuid]
  (let [req (queries/get-tool-request-details uuid)]
    (when (nil? req)
      (throw+ {:type :clojure-commons.exception/not-found
               :error (str "Could not locate tool with the following id: " (string/upper-case (.toString uuid)))}))
    req))

(defn- get-most-recent-status
  "Gets the most recent status for a tool request."
  [uuid]
  (let [status ((comp :name first)
                (select [:tool_requests :tr]
                        (fields :trsc.name)
                        (join [:tool_request_statuses :trs]
                              {:tr.id :trs.tool_request_id})
                        (join [:tool_request_status_codes :trsc]
                              {:trs.tool_request_status_code_id :trsc.id})
                        (where {:tr.id uuid})
                        (order :trs.date_assigned :DESC)
                        (limit 1)))]
    (when (nil? status)
      (throw+ {:type :clojure-commons.exception/failed-dependency
               :error "no status found for tool request"
               :id (string/upper-case (.toString uuid))}))
    status))

(defn- get-status-code
  "Attempts to retrieve a status code from the database."
  [status-code]
  (first
   (select tool_request_status_codes
           (fields :id :name :description)
           (where {:name status-code}))))

(defn- new-status-code-record
  "Creates a new status code record."
  [status-code]
  {:id          (UUID/randomUUID)
   :name        status-code
   :description status-code})

(defn- add-status-code
  "Adds a new status code."
  [status-code]
  (let [rec (new-status-code-record status-code)]
    (insert tool_request_status_codes (values rec))
    rec))

(defn- load-status-code
  "Gets status code information from the database, adding a new entry if necessary."
  [status-code]
  (or (get-status-code status-code)
      (add-status-code status-code)))

(defn- handle-tool-request-update
  "Updates a tool request."
  [update uuid uid-domain]
  (transaction
   (let [prev-status (get-most-recent-status uuid)
         status      (:status update prev-status)
         status-id   (:id (load-status-code status))
         username    (required-field update :username)
         username    (if (re-find #"@" username) username (str username "@" uid-domain))
         user-id     (queries/get-user-id username)
         comments    (:comments update)
         comments    (when-not (string/blank? comments) comments)]
     (insert tool_request_statuses
             (values {:tool_request_id             uuid
                      :tool_request_status_code_id status-id
                      :updater_id                  user-id
                      :comments                    comments}))
     uuid)))

(defn- get-tool-request-list
  [params]
  (let [limit      (params/optional-long [:limit] params)
        offset     (params/optional-long [:offset] params)
        sort-field (params/optional-keyword [:sort-field] params)
        sort-order (params/optional-keyword [:sort-dir] params)
        statuses   (params/optional-vector [:status] params)
        username   (params/optional-string [:username] params)]
    (queries/list-tool-requests :username   username
                                :limit      limit
                                :offset     offset
                                :sort-field sort-field
                                :sort-order sort-order
                                :statuses   statuses)))

(defn- format-tool-request-status
  "Formats a single status record for a tool request."
  [req-status]
  (assoc req-status
    :status_date (.getTime (:status_date req-status))
    :comments    (or (:comments req-status) "")))

(defn- get-tool-request-details
  "Retrieves the details of a single tool request from the database."
  [uuid]
  (let [req     (get-tool-req uuid)
        history (map format-tool-request-status (queries/get-tool-request-history uuid))]
    (assoc req :history history)))

(defn get-tool-request
  "Lists the details of a single tool request."
  [uuid]
  (remove-nil-vals (get-tool-request-details uuid)))

(defn- send-tool-request-notification
  [tool-request user]
  (cn/send-tool-request-notification tool-request user)
  tool-request)

(defn submit-tool-request
  "Submits a tool request on behalf of a user."
  [{:keys [username] :as user} request]
  (-> (handle-new-tool-request username request)
      (get-tool-request)
      (send-tool-request-notification user)))

(defn- send-tool-request-update-notification
  [tool-request]
  (->> (load-user (:submitted_by tool-request))
       (cn/send-tool-request-update-notification tool-request))
  tool-request)

(defn update-tool-request
  "Updates the status of a tool request."
  [uuid uid-domain {:keys [username] :as user} body]
  (-> (assoc body :username username)
      (handle-tool-request-update uuid uid-domain)
      (get-tool-request)
      (send-tool-request-update-notification)))

(defn list-tool-requests
  "Lists tool requests."
  [params]
  {:tool_requests
   (map #(assoc %
           :date_submitted (.getTime (:date_submitted %))
           :date_updated   (.getTime (:date_updated %)))
        (get-tool-request-list params))})

(defn- add-filter
  [query field filter]
  (if filter
    (where query {(sqlfn :lower field) [like (str "%" (string/lower-case filter) "%")]})
    query))

(defn list-tool-request-status-codes
  "Lists the known tool request status codes."
  [{:keys [filter]}]
  {:status_codes
   (-> (select* tool_request_status_codes)
       (fields :id :name :description)
       (order :name :ASC)
       (add-filter :name filter)
       (select))})

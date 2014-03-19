(ns metadactyl.metadata.tool-requests
  (:use [clojure.java.io :only [reader]]
        [kameleon.entities]
        [korma.core]
        [korma.db]
        [metadactyl.util.service :only [success-response]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]
            [kameleon.queries :as queries]
            [metadactyl.util.params :as params])
  (:import [java.util UUID]))

;; Status codes.
(def ^:private initial-status-code "Submitted")

(defn- required-field
  "Extracts a required field from a map."
  [m & ks]
  (let [v (first (remove string/blank? (map m ks)))]
    (when (nil? v)
      (throw+ {:code          ::missing_required_field
               :accepted_keys ks}))
    v))

(defn- multithreaded-str-to-flag
  "Converts a multithreaded indication string to a boolean flag."
  [s]
  (condp = s
    "Yes" true
    "No"  false
    nil))

(defn- architecture-name-to-id
  "Gets the internal architecture identifier for an architecture name."
  [architecture]
  (let [id (:id (first (select tool_architectures (where {:name architecture}))))]
    (when (nil? id)
      (throw+ {:code ::unknown_architecture
               :name architecture}))
    id))

(defn- status-code-subselect
  "Creates a subselect statement to find the primary key of a status code."
  [status-code]
  (subselect tool_request_status_codes
             (fields :id)
             (where {:name status-code})))

(defn- tool-request-subselect
  "Creates a subselect statement to find the primary key for a tool request UUID."
  [uuid]
  (subselect tool_requests
             (fields :id)
             (where {:uuid uuid})))

(defn- handle-new-tool-request
  "Submits a tool request on behalf of the authenticated user."
  [username req]
  (transaction
   (let [user-id         (queries/get-user-id username)
         architecture-id (architecture-name-to-id (required-field req :architecture))
         uuid            (UUID/randomUUID)]

     (insert tool_requests
             (values {:phone                (:phone req)
                      :uuid                 uuid
                      :tool_name            (required-field req :name)
                      :description          (required-field req :description)
                      :source_url           (required-field req :src_url :src_upload_file)
                      :doc_url              (required-field req :documentation_url)
                      :version              (required-field req :version)
                      :attribution          (:attribution req "")
                      :multithreaded        (multithreaded-str-to-flag (:multithreaded req))
                      :test_data_path       (required-field req :test_data_file)
                      :instructions         (required-field req :cmd_line)
                      :additional_info      (:additional_info req)
                      :additional_data_file (:additional_data_file req)
                      :requestor_id         user-id
                      :tool_architecture_id architecture-id}))

     (insert tool_request_statuses
             (values {:tool_request_id             (tool-request-subselect uuid)
                      :tool_request_status_code_id (status-code-subselect initial-status-code)
                      :updater_id                  user-id}))
     uuid)))

(defn- get-tool-req
  "Loads a tool request from the database."
  [uuid]
  (let [req (first (select tool_requests (where {:uuid uuid})))]
    (when (nil? req)
      (throw+ {:code ::tool_request_not_found
               :uuid (string/upper-case (.toString uuid))}))
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
                        (where {:tr.uuid uuid})
                        (order :trs.date_assigned :DESC)
                        (limit 1)))]
    (when (nil? status)
      (throw+ {:code ::no_status_found_for_tool_request
               :uuid (string/upper-case (.toString uuid))}))
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
  [uid-domain update]
  (transaction
   (let [uuid        (UUID/fromString (required-field update :uuid))
         req-id      (:id (get-tool-req uuid))
         prev-status (get-most-recent-status uuid)
         status      (:status update prev-status)
         status-id   (:id (load-status-code status))
         username    (required-field update :username)
         username    (if (re-find #"@" username) username (str username "@" uid-domain))
         user-id     (queries/get-user-id username)
         comments    (:comments update)
         comments    (when-not (string/blank? comments) comments)]
     (insert tool_request_statuses
             (values {:tool_request_id             req-id
                      :tool_request_status_code_id status-id
                      :updater_id                  user-id
                      :comments                    comments}))
     uuid)))

(defn- get-tool-request-list
  [params]
  (let [limit      (params/optional-long [:limit] params)
        offset     (params/optional-long [:offset] params)
        sort-field (params/optional-keyword [:sortfield :sortField] params)
        sort-order (params/optional-keyword [:sortdir :sortDir] params)
        statuses   (params/optional-vector [:status] params)
        username   (params/optional-string [:username] params)]
    (queries/list-tool-requests :username   username
                                :limit      limit
                                :offset     offset
                                :sort-field sort-field
                                :sort-order sort-order
                                :statuses   statuses)))

(def ^:private format-uuid
  "Formats a UUID."
  (comp string/upper-case str))

(def ^:private format-timestamp
  "Formats a timestamp."
  (comp str #(.getTime %)))

(defn- format-tool-request
  "Formats a tool request."
  [req]
  (update-in req [:uuid] format-uuid))

(defn- format-tool-request-status
  "Formats a single status record for a tool request."
  [req-status]
  (assoc req-status
    :status_date (format-timestamp (:status_date req-status))
    :comments    (or (:comments req-status) "")))

(defn- get-tool-request-details
  "Retrieves the details of a single tool request from the database."
  [uuid]
  (let [req     (format-tool-request (queries/get-tool-request-details uuid))
        history (map format-tool-request-status (queries/get-tool-request-history uuid))]
    (assoc req :history history)))

(defn submit-tool-request
  "Submits a tool request on behalf of a user."
  [username body]
  (-> (handle-new-tool-request username (cheshire/decode-stream (reader body) true))
      (get-tool-request-details)
      (success-response)))

(defn update-tool-request
  "Updates the status of a tool request."
  ([uid-domain body]
     (->> (cheshire/decode-stream (reader body) true)
          (handle-tool-request-update uid-domain)
          (get-tool-request-details)
          (success-response)))
  ([uid-domain username body]
     (->> (assoc (cheshire/decode-stream (reader body) true) :username username)
          (handle-tool-request-update uid-domain)
          (success-response))))

(defn get-tool-request
  "Lists the details of a single tool request."
  [uuid]
  (success-response (get-tool-request-details (UUID/fromString uuid))))

(defn list-tool-requests
  "Lists tool requests."
  [params]
  (success-response
   {:tool_requests
    (map #(assoc %
            :uuid           (format-uuid (:uuid %))
            :date_submitted (format-timestamp (:date_submitted %))
            :date_updated   (format-timestamp (:date_updated %)))
         (get-tool-request-list params))}))

(defn- add-filter
  [query field filter]
  (if filter
    (where query {(sqlfn :lower field) [like (str "%" (string/lower-case filter) "%")]})
    query))

(defn list-tool-request-status-codes
  "Lists the known tool request status codes."
  [{:keys [filter]}]
  (success-response
   {:status_codes
    (-> (select* tool_request_status_codes)
        (fields :id :name :description)
        (order :name :ASC)
        (add-filter :name filter)
        (select))}))

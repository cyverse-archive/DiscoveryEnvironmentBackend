(ns donkey.services.metadata.metadactyl
  (:use [clojure.java.io :only [reader]]
        [donkey.util.config]
        [donkey.util.transformers]
        [donkey.auth.user-attributes]
        [donkey.clients.user-info :only [get-user-details]]
        [donkey.persistence.workspaces :only [get-or-create-workspace]]
        [donkey.services.user-prefs :only [user-prefs]]
        [donkey.util.email]
        [donkey.util.service]
        [kameleon.queries :only [record-login record-logout]]
        [korma.db :only [with-db]]
        [medley.core :only [dissoc-in]]
        [ring.util.codec :only [url-encode]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [donkey.clients.data-info :as di]
            [donkey.clients.metadactyl :as dm]
            [donkey.clients.notifications :as dn]
            [donkey.util.db :as db]
            [donkey.services.fileio.actions :as io]))

(defn- secured-notification-url
  [req & components]
  (apply build-url-with-query (notificationagent-base)
         (add-current-user-to-map (:params req)) components))

(defn- secured-params
  ([]
   (secured-params {}))
  ([existing-params]
   (add-current-user-to-map existing-params)))

(defn- metadactyl-request
  "Prepares a metadactyl request by extracting only the body of the client request and sets the
   forwarded request's content-type to json."
  [req]
  (assoc (select-keys req [:body]) :content-type :json))

(defn- metadactyl-url
  "Adds the name and email of the currently authenticated user to the metadactyl URL with the given
   relative URL path."
  [query & components]
  (apply build-url-with-query (metadactyl-base)
                              (secured-params query)
                              components))

(defn get-all-workflow-elements
  "A service to get information about all workflow elements."
  [params]
  (let [params (select-keys params [:include-hidden])]
    (client/get (metadactyl-url params "apps" "elements")
                {:as :stream})))

(defn get-workflow-elements
  "A service to get information about selected workflow elements."
  [element-type params]
  (let [params (select-keys params [:include-hidden])]
    (client/get (metadactyl-url params "apps" "elements" element-type)
                {:as :stream})))

(defn search-tools
  "A service to search information about tools."
  [{params :params :as req}]
  (let [params (select-keys params (conj dm/metadactyl-sort-params :search :include-hidden))
        url    (metadactyl-url params "tools")
        req    (metadactyl-request req)]
    (forward-get url req)))

(defn get-tool
  "This service will get a tool by ID."
  [req tool-id]
  (let [url (metadactyl-url {} "tools" tool-id)
        req (metadactyl-request req)]
    (forward-get url req)))

(defn import-tools
  "This service will import deployed components into the DE and send
   notifications if notification information is included and the deployed
   components are successfully imported."
  [body]
  (let [json (decode-json body)]
    (dm/import-tools json)
    (dorun (map dn/send-tool-notification (:tools json))))
  (success-response))

(defn update-app-labels
  "This service updates the labels in a single-step app. Both vetted and unvetted apps can be
   modified using this service."
  [req app-id]
  (let [url (metadactyl-url {} "apps" app-id)
        req (metadactyl-request req)]
    (forward-patch url req)))

(defn bootstrap
  "This service obtains information about and initializes the workspace for the authenticated user.
   It also records the fact that the user logged in."
  [{{:keys [ip-address]} :params {user-agent "user-agent"} :headers}]
  (assert-valid ip-address "Missing or empty query string parameter: ip-address")
  (assert-valid user-agent "Missing or empty request parameter: user-agent")
  (let [username    (:username current-user)
        user        (:shortUsername current-user)
        workspace   (get-or-create-workspace username)
        preferences (user-prefs (:username current-user))
        login-time  (with-db db/de
                      (record-login username ip-address user-agent))]
    (success-response
      {:workspaceId   (:id workspace)
       :newWorkspace  (:newWorkspace workspace)
       :loginTime     (str login-time)
       :username      user
       :full_username username
       :email         (:email current-user)
       :firstName     (:firstName current-user)
       :lastName      (:lastName current-user)
       :userHomePath  (di/user-home-folder user)
       :userTrashPath (di/user-trash-folder user)
       :baseTrashPath (di/base-trash-folder)
       :preferences   preferences})))

(defn logout
  "This service records the fact that the user logged out."
  [{:keys [ip-address login-time]}]
  (assert-valid ip-address "Missing or empty query string parameter: ip-address")
  (assert-valid login-time "Missing or empty query string parameter: login-time")
  (with-db db/de
    (record-logout (:username current-user)
                   ip-address
                   (string->long login-time "Long integer expected: login-time")))
  (success-response))

(defn get-messages
  "This service forwards requests to the notification agent in order to
   retrieve notifications that the user may or may not have seen yet."
  [req]
  (forward-get (dn/notificationagent-url "messages" (:params req)) req))

(defn get-unseen-messages
  "This service forwards requests to the notification agent in order to
   retrieve notifications that the user hasn't seen yet."
  [req]
  (forward-get (dn/notificationagent-url "unseen-messages") req))

(defn last-ten-messages
  "This service forwards requests for the ten most recent notifications to the
   notification agent."
  [req]
  (forward-get (dn/notificationagent-url "last-ten-messages" (:params req)) req))

(defn count-messages
  "This service forwards requests to the notification agent in order to
   retrieve the number of notifications satisfying the conditions in the
   query string."
  [req]
  (let [url (dn/notificationagent-url "count-messages" (:params req))]
    (forward-get url req)))

(defn delete-notifications
  "This service forwards requests to the notification agent in order to delete
   existing notifications."
  [req]
  (let [url (dn/notificationagent-url "delete")]
    (forward-post url req)))

(defn delete-all-notifications
  "This service forwards requests to the notification agent in order to delete
   all notifications for the user."
  [params]
  (let [url (dn/notificationagent-url "delete-all" params)]
    (forward-delete url params)))

(defn mark-notifications-as-seen
  "This service forwards requests to the notification agent in order to mark
   notifications as seen by the user."
  [req]
  (let [url (dn/notificationagent-url "seen")]
    (forward-post url req)))

(defn mark-all-notifications-seen
  "This service forwards requests to the notification agent in order to mark all
   notifications as seen for the user."
  [req]
  (let [url (dn/notificationagent-url "mark-all-seen")]
    (forward-post url req (cheshire/encode (add-current-user-to-map {})))))

(defn send-notification
  "This service forwards a notifiction to the notification agent's general
   notification endpoint."
  [req]
  (let [url (dn/notificationagent-url "notification")]
    (forward-post url req)))

(defn get-system-messages
  "This service forwards a notification to the notification agent's endpoint
   for retrieving system messages."
  [req]
  (forward-get (secured-notification-url req "system" "messages") req))

(defn get-new-system-messages
  "Forwards a request to the notification agent's endpoint for getting new system messages."
  [req]
  (forward-get (secured-notification-url req "system" "new-messages") req))

(defn get-unseen-system-messages
  "Forwards a request to the notification agent's endpoint for getting
   unseen system messages."
  [req]
  (forward-get (secured-notification-url req "system" "unseen-messages") req))

(defn mark-system-messages-received
  "Forwards a request to the notification to mark a set of system notifications as received."
  [req]
  (forward-post (secured-notification-url req "system" "received") req))

(defn mark-all-system-messages-received
  "Forwards a request to the notification-agent to mark all system notifications as received."
  [req]
  (forward-post (secured-notification-url req "system" "mark-all-received") req))

(defn mark-system-messages-seen
  "Forwards a request to the notification to mark a set of system notifications
   as seen."
  [req]
  (forward-post (secured-notification-url req "system" "seen") req))

(defn mark-all-system-messages-seen
  "Forwards a request to the notification-agent to mark all system notifications as seen."
  [req]
  (forward-post (secured-notification-url req "system" "mark-all-seen") req))

(defn delete-system-messages
  "Forwards a request to the notification-agent to soft-delete a set of system messages."
  [req]
  (forward-post (secured-notification-url req "system" "delete") req))

(defn delete-all-system-messages
  "Forwards a request to to the notification-agent to soft-delete all system messages for a
   set of users."
  [req]
  (forward-delete (secured-notification-url req "system" "delete-all") req))

(defn admin-add-system-message
  "Forwards a request to the notification-agent to allow an admin to add a new system
   message."
  [req]
  (forward-put (secured-notification-url req "admin" "system") req))

(defn admin-list-system-types
  "Forwards a request to the notification-agent to allow an admin to list the current
   list of system notification types."
  [req]
  (forward-get (secured-notification-url req "admin" "system-types") req))

(defn admin-list-system-messages
  "Forwards a request to the notification agent to allow an admin to list existing system
   notifications."
  [req]
  (forward-get (secured-notification-url req "admin" "system") req))

(defn admin-get-system-message
  "Forwards a request to the notification-agent to get a system notification for an admin."
  [req uuid]
  (forward-get (secured-notification-url req "admin" "system" uuid) req))

(defn admin-update-system-message
  "Forwards a request to the notification-agent to update a system notification for an admin."
  [req uuid]
  (forward-post (secured-notification-url req "admin" "system" uuid) req))

(defn admin-delete-system-message
  "Forwards a request to the notification-agent to delete a system notification for an admin."
  [req uuid]
  (forward-delete (secured-notification-url req "admin" "system" uuid) req))

(defn list-reference-genomes
  "Lists the reference genomes in the database."
  [params]
  (client/get (metadactyl-url (select-keys params [:deleted]) "reference-genomes")
              {:as :stream}))

(defn get-reference-genome
  "Gets a reference genome by its UUID."
  [reference-genome-id]
  (client/get (metadactyl-url {} "reference-genomes" reference-genome-id)
              {:as :stream}))

(defn add-reference-genome
  "Adds a reference genome via metadactyl."
  [req]
  (let [url (metadactyl-url {} "admin" "reference-genomes")
        req (metadactyl-request req)]
    (forward-post url req)))

(defn replace-reference-genomes
  "Replaces the reference genomes in the database with a new set of reference genomes."
  [req]
  (let [url (metadactyl-url {} "admin" "reference-genomes")
        req (metadactyl-request req)]
    (forward-put url req)))

(defn delete-reference-genomes
  "Logically deletes a reference genome in the database."
  [reference-genome-id]
  (client/delete (metadactyl-url {} "admin" "reference-genomes" reference-genome-id)
                 {:as :stream}))

(defn update-reference-genome
  "Updates a reference genome via metadactyl."
  [req reference-genome-id]
  (let [url (metadactyl-url {} "admin" "reference-genomes" reference-genome-id)
        req (metadactyl-request req)]
    (forward-patch url req)))

(defn- extract-uploaded-path
  "Gets the file ID as a path from the given upload results."
  [upload]
  (get-in upload [:file :id]))

(defn- upload-tool-request-file
  "Uploads a file with a tmp path, found in params by the given file-key, to the
   given user's final-path dir, then updates file-key in params with the file's
   new path."
  [params file-key user final-path]
  (let [tmp-path (params file-key)]
    (if (nil? tmp-path)
      params
      (assoc params
             file-key
             (extract-uploaded-path (io/upload user tmp-path final-path))))))


(defn- postprocess-tool-request
  "Postprocesses a tool request update or submission. The postprocessing function
   should take the tool request and user details as arguments."
  [res f]
  (if (<= 200 (:status res) 299)
    (let [tool-req     (cheshire/decode-stream (reader (:body res)) true)
          username     (string/replace (:submitted_by tool-req) #"@.*" "")
          user-details (get-user-details username)]
      (f tool-req user-details))
    res))

(defn submit-tool-request
  "Submits a tool request on behalf of the user found in the request params."
  [req]
  (let [tool-request-url (metadactyl-url {} "tool-requests")
        req (metadactyl-request req)]
    (postprocess-tool-request
      (forward-post tool-request-url req)
      (fn [tool-req user-details]
        (send-tool-request-email tool-req user-details)
        (dn/send-tool-request-notification tool-req user-details)
        (success-response tool-req)))))

(defn list-tool-requests
  "Lists the tool requests that were submitted by the authenticated user."
  []
  (client/get (metadactyl-url {} "tool-requests")
              {:as :stream}))

(defn admin-list-tool-requests
  "Lists the tool requests that were submitted by any user."
  [params]
  (success-response (dm/admin-list-tool-requests params)))

(defn list-tool-request-status-codes
  "Lists the known tool request status codes."
  [params]
  (success-response (dm/list-tool-request-status-codes params)))

(defn update-tool-request
  "Updates a tool request with comments and possibly a new status."
  [req request-id]
  (let [url (metadactyl-url {} "admin" "tool-requests" request-id "status")
        req (metadactyl-request req)]
    (postprocess-tool-request
      (forward-post url req)
      (fn [tool-req user-details]
        (dn/send-tool-request-update-notification tool-req user-details)
        (success-response tool-req)))))

(defn get-tool-request
  "Lists details about a specific tool request."
  [request-id]
  (client/get (metadactyl-url {} "admin" "tool-requests" request-id)
              {:as :stream}))

(defn provide-user-feedback
  "Forwards feedback from the user to iPlant."
  [body]
  (send-feedback-email (cheshire/decode-stream (reader body)))
  (success-response))

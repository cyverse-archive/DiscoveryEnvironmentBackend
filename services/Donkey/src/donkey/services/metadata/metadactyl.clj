(ns donkey.services.metadata.metadactyl
  (:use [clojure.java.io :only [reader]]
        [clojure-commons.client :only [build-url-with-query]]
        [donkey.util.config]
        [donkey.util.transformers :only [secured-params add-current-user-to-map]]
        [donkey.auth.user-attributes]
        [donkey.services.user-prefs :only [user-prefs]]
        [donkey.util.email]
        [donkey.util.service])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [donkey.clients.iplant-groups :as ipg]
            [donkey.clients.data-info :as di]
            [donkey.clients.metadactyl :as dm]
            [donkey.clients.notifications :as dn]
            [donkey.services.fileio.actions :as io]))

(defn- secured-notification-url
  [req & components]
  (apply build-url-with-query (notificationagent-base)
         (add-current-user-to-map (:params req)) components))

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

(defn import-tools
  "This service will import deployed components into the DE and send
   notifications if notification information is included and the deployed
   components are successfully imported."
  [body]
  (let [json (decode-json body)]
    (dm/import-tools json)
    (dorun (map dn/send-tool-notification (:tools json))))
  (success-response))

(defn bootstrap
  "This service obtains information about and initializes the workspace for the authenticated user.
   It also records the fact that the user logged in."
  [{{:keys [ip-address]} :params {user-agent "user-agent"} :headers}]
  (assert-valid ip-address "Missing or empty query string parameter: ip-address")
  (assert-valid user-agent "Missing or empty request parameter: user-agent")
  (let [username    (:username current-user)
        user        (:shortUsername current-user)
        workspace   (dm/get-workspace)
        preferences (user-prefs (:username current-user))
        login-time  (:login_time (dm/record-login ip-address user-agent))]
    (success-response
      {:workspaceId   (:id workspace)
       :newWorkspace  (:new_workspace workspace)
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
  (dm/record-logout ip-address login-time)
  (success-response))

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
          user-details (ipg/format-like-trellis (ipg/lookup-subject-add-empty username username))]
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

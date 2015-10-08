(ns notification-agent.delete
  (:use [notification-agent.common]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [notification-agent.db :as db]))

(defn delete-messages
  "Handles a message deletion request.  The request body should consist of
   a JSON array of message UUIDs."
  [params body]
  (log/debug "handling a notification message deletion request")
  (let [user    (validate-user (:user params))
        request (parse-body body)]
    (if (and (map? request) (vector? (:uuids request)))
      (do
        (db/delete-notifications user (:uuids request))
        nil)
      (throw+ {:type         :clojure-commons.exception/bad-request-field
               :field_name   :uuids
               :request_body body}))))

(defn delete-all-messages
  "Handles a request to delete all messages for a specific user that match"
  [params]
  (log/debug "handling a notification delete-all request")
  (let [user (validate-user (:user params))]
    (log/debug "deleting notifications for" user)
    (db/delete-matching-notifications user params)
    {:count (str (db/count-matching-messages user {:seen false}))}))

(defn delete-system-messages
  "Handles the deletion of system messages."
  [params body]
  (log/debug "handling a system notification deletion request")
  (let [user (validate-user (:user params))
        request (parse-body body)]
    (if (and (map? request) (vector? (:uuids request)))
      (do
        (db/soft-delete-system-notifications user (:uuids request))
        {:count (str (db/count-active-system-notifications user))})
      (throw+ {:type         :clojure-commons.exception/bad-request-field
               :field_name   :uuids
               :request_body body}))))

(defn delete-all-system-messages
  "Handles the deletion of system messages for a particular user."
  [params]
  (log/debug "handling a system notification delete-all request")
  (let [user (validate-user (:user params))]
    (log/debug "deleting system notifications for " user)
    (db/soft-delete-all-system-notifications user)
    {:count (str (db/count-active-system-notifications user))}))

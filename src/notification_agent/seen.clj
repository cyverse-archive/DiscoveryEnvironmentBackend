(ns notification-agent.seen
  "This namespace provides the endpoint processing logic for marking messages as received or seen."
  (:use [notification-agent.common]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [notification-agent.db :as db]))

(defn- validate-uuids
  "Validates the list of UUIDs that was passed in."
  [uuids body]
  (when (or (nil? uuids) 
            (not (coll? uuids)))
    (throw+ {:error_code ce/ERR_BAD_OR_MISSING_FIELD
             :field_name :uuids
             :body       body})))

(defn- successful-seen-response
  "Returns the response for a successful request to mark messages seen."
  [user]
  (success-resp {:count (str (db/count-matching-messages user {:seen false}))}))

(defn mark-messages-seen
  "Marks one or more notification messages as seen."
  [body {:keys [user]}]
  (validate-user user)
  (let [uuids (:uuids (parse-body body))]
    (validate-uuids uuids body)
    (db/mark-notifications-seen user uuids)
    (successful-seen-response user)))

(defn mark-all-messages-seen
  "Marks all notification messages as seen."
  [body]
  (let [user (validate-user (:user (parse-body body)))]
    (db/mark-matching-notifications-seen user {:seen false})
    (successful-seen-response user)))

(defn mark-system-messages-received
  "Marks one or more system notifications as being received by a given user.

   Parameters:
     body   - The body of the HTTP post as formatted by ring
     params - The query parameters as formatted by ring

   Returns:
     It returns the number of system notifications that have not be marked as received by the
     given user.  The return is formatted as a map that ring can use to format an HTTP response."
  [body {:keys [user]}]
  (validate-user user)
  (let [uuids (:uuids (parse-body body))]
    (validate-uuids uuids body)
    (db/mark-system-notifications-received user uuids)
    (success-resp {:count (str (db/count-new-system-notifications user))})))

(defn mark-all-system-messages-received
  "Marks all system messages as being received by a given user.

   Parameters:
     body   - The body of the HTTP post as formatted by ring

   Returns:
     It returns the number of system notifications that have not be marked as received by the
     given user.  The return is formatted as a map that ring can use to format an HTTP response."
  [body]
  (let [user (validate-user (:user (parse-body body)))]
    (db/mark-all-system-notifications-received user)
    (success-resp {:count (str (db/count-new-system-notifications user))})))

(defn mark-system-messages-seen
  "Marks one or more system notifications as seen."
  [body {:keys [user]}]
  (validate-user user)
  (let [uuids (:uuids (parse-body body))]
    (validate-uuids uuids body)
    (db/mark-system-notifications-seen user uuids)
    (success-resp {:count (str (db/count-unseen-system-notifications user))})))

(defn mark-all-system-messages-seen
  "Marks all system notifications as seen for a user."
  [body]
  (let [user (validate-user (:user (parse-body body)))]
    (db/mark-all-system-notifications-seen user)
    (success-resp {:count (str (db/count-unseen-system-notifications user))})))

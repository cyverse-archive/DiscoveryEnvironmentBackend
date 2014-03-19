(ns notification-agent.messages
  (:use [notification-agent.config]
        [notification-agent.messages]
        [notification-agent.time]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure-commons.osm :as osm]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [notification-agent.db :as db])
  (:import [java.io IOException]
           [java.util Comparator]))

(defn- fix-timestamp
  "Some timestamps are stored in the default timestamp format used by
   JavaScript.  The DE needs all timestamps to be represented as milliseconds
   since the epoch.  This function fixes timestamps that are in the wrong
   format."
  [timestamp]
  (let [ts (str timestamp)]
    (if (re-matches #"^\d*$" ts) ts (str (timestamp->millis ts)))))

(defn- opt-update-in
  "Updates a value in a map if that value exists."
  [m ks f & args]
  (let [value (get-in m ks)]
    (if (nil? value) m (apply update-in m ks f args))))

(defn reformat-message
  "Converts a message from the format stored in the OSM to the format that the
   DE expects."
  [uuid state & {:keys [seen deleted] :or {seen false deleted false}}]
  (-> state
    (assoc-in [:message :id] uuid)
    (opt-update-in [:message :timestamp] fix-timestamp)
    (opt-update-in [:payload :startdate] fix-timestamp)
    (opt-update-in [:payload :enddate] fix-timestamp)
    (dissoc :email_request)
    (assoc :seen seen :deleted deleted)
    (assoc :type (string/replace (or (:type state) "") #"_" " "))))

(defn- send-email-request
  "Sends an e-mail request to the iPlant e-mail service."
  [notification-uuid {:keys [template to] :as request}]
  (log/debug "sending an e-mail request:" request)
  (let [json-request (cheshire/encode request)]
    (client/post (email-url)
                 {:body         json-request
                  :content-type :json})
    (db/record-email-request notification-uuid template to json-request)))

(defn- persist-msg
  "Persists a message in the OSM."
  [{type :type username :user {subject :text created-date :timestamp} :message :as msg}]
  (log/debug "saving a message in the OSM:" msg)
  (db/insert-notification
   (or type "analysis") username subject created-date (cheshire/encode msg)))

(defn- send-msg-to-recipient
  "Forawards a message to a single recipient."
  [url msg]
  (log/debug "sending message to" url)
  (try
    (client/post url {:body msg})
    (catch IOException e
      (log/error e "unable to send message to" url))))

(defn- send-msg
  "Forwards a message to zero or more recipients."
  [msg]
  (let [recipients (notification-recipients)]
    (log/debug "forwarding message to" (count recipients) "recipients")
    (dorun (map #(send-msg-to-recipient % msg) recipients))))

(defn persist-and-send-msg
  "Persists a message in the OSM and sends it to any receivers and returns
   the state object."
  [msg]
  (let [uuid          (persist-msg msg)
        email-request (:email_request msg)]
    (log/debug "UUID of persisted message:" uuid)
    (when-not (nil? email-request)
      (send-email-request uuid email-request))
    (send-msg (cheshire/encode (reformat-message uuid msg)))))

(defn- optional-insert-system-args
  [msg]
  (->> [[:activation_date (str (timestamp->millis (:activation_date msg)))]
        [:dismissible     (:dismissible msg)]
        [:logins_disabled (:logins_disabled msg)]]
       (remove (fn [[_ v]] (nil? v)))
       (flatten)))

(defn persist-system-msg
  "Persists a system notification in the database."
  [msg]
  (let [type                (:type msg)
        ddate               (str (timestamp->millis (:deactivation_date msg)))
        message             (:message msg)
        insert-system-notif (partial db/insert-system-notification type ddate message)
        sys-args            (optional-insert-system-args msg)]
    {:system-notification
     (if (pos? (count sys-args))
       (apply insert-system-notif sys-args)
       (insert-system-notif))}))

(defn list-system-msgs
  [active-only type limit offset]
  {:system-messages (db/list-system-notifications active-only type limit offset)
   :total           (db/count-system-notifications active-only type)})

(defn get-system-msg
  [uuid]
  {:system-notification (db/get-system-notification-by-uuid uuid)})

(defn update-system-msg
  [uuid update-map]
  {:system-notification (db/update-system-notification uuid update-map)})

(defn delete-system-msg
  [uuid]
  {:system-notification (db/delete-system-notification uuid)})

(defn get-system-msg-types
  []
  {:types (db/get-system-notification-types)})

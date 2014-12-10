(ns notification-agent.notifications
  (:use [notification-agent.common]
        [notification-agent.config]
        [notification-agent.messages]
        [notification-agent.time])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn- validate-long
  "Validates a long integer argument."
  [arg-name arg-value]
  (when arg-value
    (string->long
     arg-value
     ::invalid-long-integer-param
     {:param (name arg-name) :value arg-value})))

(defn- validate-field
  "Verifies that a required field is present in a possibly nested map
   structure."
  [m ks]
  (if (string/blank? (get-in m ks))
    (let [desc (string/join " -> " ks)]
      (throw
        (IllegalArgumentException. (str "missing required field " desc))))))

(defn- validate-time
  "Verifies that time when a system message is active has a positive duration and is not entirely in
   the past."
  [req]
  (let [end-time (timestamp->millis (:deactivation_date req))]
  (when (<= end-time (System/currentTimeMillis))
    (throw (IllegalArgumentException. "The provided deactivation date is in the past.")))
  (when-let [beg-time (:activation_date req)]
    (when (<= end-time (timestamp->millis beg-time))
      (throw (IllegalArgumentException.
               "The activation date needs to be prior to the deactivation date."))))))

(def
  ^{:private true
    :doc "The fields that have to be present in the request body."}
  required-fields
  [[:type] [:user] [:subject]])

(def ^{:private true} required-system-fields
  [[:type] [:deactivation_date] [:message]])

(defn- validate-request
  "Verifies that an incoming message request contains all of the required
   fields."
  [msg & {:keys [req-fields] :or {req-fields required-fields}}]
  (dorun (map (partial validate-field msg) req-fields)))

(defn- request-to-msg
  "Converts a notification request to a full-fledged message that will be stored
  in the notification database."
  [request]
  {:type           (:type request)
   :user           (:user request)
   :subject        (:subject request)
   :email          (:email request false)
   :email_template (:email_template request)
   :payload        (:payload request {})
   :message        {:id        ""
                    :timestamp (str (System/currentTimeMillis))
                    :text      (:message request (:subject request))}})

(defn- email-request
  "Formats an e-mail request for a generic notification."
  [addr template subject payload]
  {:to       addr
   :template template
   :subject  subject
   :values   payload})

(defn- send-email?
  "Determines whether or not an e-mail should be sent to the user."
  [email-requested template addr]
  (and (email-enabled) email-requested (not (string/blank? template))
       (valid-email-addr addr)))

(defn- add-email-request
  "Adds an e-mail request to the notification message if an e-mail is
   requested."
  [{{addr :email_address :as payload} :payload template :email_template subject :subject :as msg}]
  (if (send-email? (:email msg) template addr)
    (assoc msg :email_request (email-request addr template subject payload))
    msg))

(defn handle-notification-request
  "Handles a general notification request."
  [body]
  (let [request (parse-body body)]
    (validate-request request)
    (persist-and-send-msg (add-email-request (request-to-msg request)))
    (success-resp)))

(defn handle-add-system-notif
  "Handles a system notification request."
  [body]
  (let [request (parse-body body)]
    (validate-request request :req-fields required-system-fields)
    (validate-time request)
    (persist-system-msg request)))

(defn handle-system-notification-listing
  "Handles a system notification listing request."
  [{:keys [active-only type limit offset]}]
  (validate-long :limit limit)
  (validate-long :offset offset)
  (list-system-msgs (Boolean/parseBoolean active-only) type limit offset))

(defn handle-get-system-notif
  "Handles getting a system notification."
  [uuid]
  (parse-uuid uuid)
  (get-system-msg uuid))

(defn handle-update-system-notif
  "Handles updating a system notification."
  [uuid body]
  (let [request (parse-body body)]
    (parse-uuid uuid)
    (update-system-msg uuid request)))

(defn handle-delete-system-notif
  "Handles deleting a system notification."
  [uuid]
  (parse-uuid uuid)
  (delete-system-msg uuid))

(defn handle-get-system-notif-types
  "Handles getting all of the types of system notifications."
  []
  (get-system-msg-types))

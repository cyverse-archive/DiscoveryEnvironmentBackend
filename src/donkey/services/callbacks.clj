(ns donkey.services.callbacks
  "Service implementations for receiving callbacks from external services."
  (:use [donkey.util.service :only [decode-json parse-form]])
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [donkey.services.metadata.apps :as apps]
            [donkey.util.service :as service]))

(defn- update-de-job-status
  [msg]
  (let [{:keys [id status enddate]} (:payload msg)]
    (apps/update-de-job-status id status enddate)))

(def ^:private notification-actions
  "Maps notification action codes to notifications."
  {:job_status_change update-de-job-status})

(defn receive-notification
  "Receives callbacks from the notification agent."
  [body]
  (let [msg       (decode-json body)
        action    (keyword (get-in msg [:payload :action]))
        action-fn (notification-actions action)]
    (when-not (nil? action-fn)
      (action-fn msg))
    (service/success-response)))

(defn receive-agave-job-status-update
  "Receives notification from Agave that a job status has changed."
  [uuid]
  (apps/update-agave-job-status uuid)
  (service/success-response))

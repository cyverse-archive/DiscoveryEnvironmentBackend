(ns donkey.services.callbacks
  "Service implementations for receiving callbacks from external services."
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [donkey.services.metadata.apps :as apps]
            [donkey.util.service :as service]))

(def ^:private notification-actions
  "Maps notification action codes to notifications."
  {})

(defn receive-notification
  "Receives callbacks from the notification agent."
  [body]
  (let [msg       (service/decode-json body)
        action    (keyword (get-in msg [:payload :action]))
        action-fn (notification-actions action)]
    (when action-fn
      (action-fn msg))
    (service/success-response)))

(defn receive-de-job-status-update
  "Receives notification from the OSM that a job status has changed."
  [body]
  (let [{:keys [state]}       (service/decode-json body)
        {:keys [status uuid]} state
        end-date              (:completion_date state)]
    (service/assert-valid uuid "no job UUID provided")
    (service/assert-valid status "no status provided")
    (apps/update-de-job-status uuid status end-date)))

(defn receive-agave-job-status-update
  "Receives notification from Agave that a job status has changed."
  [uuid {:keys [status external-id end-time]}]
  (apps/update-agave-job-status uuid status end-time external-id)
  (service/success-response))

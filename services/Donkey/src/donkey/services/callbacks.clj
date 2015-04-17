(ns donkey.services.callbacks
  "Service implementations for receiving callbacks from external services."
  (:require [clojure.tools.logging :as log]
            [donkey.util.service :as service]))

(def ^:private notification-actions
  "Maps notification action codes to notifications."
  {})

(defn receive-notification
  "Receives callbacks from the notification agent."
  [body]
  (let [msg       (log/spy (service/decode-json body))
        action    (keyword (get-in msg [:payload :action]))
        action-fn (notification-actions action)]
    (when action-fn
      (action-fn msg))
    (service/success-response)))

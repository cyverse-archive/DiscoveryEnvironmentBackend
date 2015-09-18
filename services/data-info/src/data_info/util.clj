(ns data-info.util
  "Utility functions for defining services in data-info. This namespace is used by data-info.core
   and several other top-level service definition namespaces."
  (:require [clojure.tools.logging :as log]))


(defn req-logger
  [handler]
  (fn [req]
    (log/info "REQUEST:" (dissoc req
                           :body
                           :compojure.api.middleware/options
                           :ring.swagger.middleware/data))
    (let [resp (handler req)]
      (log/info "RESPONSE:" (dissoc resp :body))
      resp)))

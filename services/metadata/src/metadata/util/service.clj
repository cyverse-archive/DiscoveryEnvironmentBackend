(ns metadata.util.service
  (:use [ring.util.response :only [charset]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [metadata.util.config :as config]
            [trptcolin.versioneer.core :as versioneer]))

(def ^:private default-content-type-header
  {"Content-Type" "application/json; charset=utf-8"})

(defn success-response
  ([map]
    (charset
      {:status  200
       :body    map
       :headers default-content-type-header}
      "UTF-8"))
  ([]
    (success-response nil)))

(defn trap
  "Traps a service call, automatically calling success-response on the result."
  [action func & args]
  (ce/trap action #(success-response (apply func args))))

(defn req-logger
  [handler]
  (fn [req]
    (log/info "REQUEST:" (dissoc req :body))
    (let [resp (handler req)]
      (log/info "RESPONSE:" (dissoc resp :body))
      resp)))

(defn get-status
  "Returns a service status map."
  []
  (let [{:keys [app-name desc group-id art-id]} config/svc-info]
    {:service      app-name
     :description  desc
     :version      (versioneer/get-version group-id art-id)}))

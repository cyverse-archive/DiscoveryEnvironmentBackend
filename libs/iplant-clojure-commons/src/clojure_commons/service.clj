(ns clojure-commons.service
  (:require [trptcolin.versioneer.core :as versioneer]))

(defn get-status
  "Returns a service status map."
  [{:keys [app-name desc group-id art-id] :as svc-info}]
  {:service      app-name
   :description  desc
   :version      (versioneer/get-version group-id art-id)})

(defn get-docs-status
  "Returns a service status map."
  [svc-info server-name server-port docs-uri]
  (merge (get-status svc-info)
    {:docs-url      (str "http://" server-name ":" server-port docs-uri)}))

(ns donkey.routes.coge
  (:use [compojure.core]
        [donkey.services.coge]
        [donkey.util.service]
        [donkey.util])
  (:require [clojure.tools.logging :as log]
            [donkey.util.config :as config]))

(defn coge-routes
  []
  (optional-routes
    [config/coge-enabled]
    (POST "/coge/genomes/load" [:as {:keys [body]}]
          (success-response (log/spy :warn (get-genome-viewer-url body))))))

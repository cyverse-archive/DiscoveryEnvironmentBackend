(ns donkey.routes.coge
  (:use [compojure.core]
        [donkey.services.coge]
        [donkey.util.service]
        [donkey.util])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]))

(defn secured-coge-routes
  []
  (optional-routes
    [config/coge-enabled]
    (POST "/coge/load-genomes" [:as {:keys [uri body]}]
          (ce/trap uri #(get-genome-viewer-url body)))))

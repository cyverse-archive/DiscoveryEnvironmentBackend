(ns donkey.routes.coge
  (:use [compojure.core]
        [donkey.services.coge]
        [donkey.util.service]
        [donkey.util])
  (:require [donkey.util.config :as config]))

(defn coge-routes
  []
  (optional-routes
    [config/coge-enabled]
    (GET "/coge/genomes" [:as {:keys [params]}]
         (success-response (search-genomes params)))

    (POST "/coge/genomes/:genome-id/export-fasta" [genome-id :as {:keys [params]}]
          (success-response (export-fasta genome-id params)))

    (POST "/coge/genomes/load" [:as {:keys [body]}]
          (success-response (get-genome-viewer-url body)))))

(ns donkey.routes.coge
  (:use [compojure.core]
        [donkey.services.coge]
        [donkey.util.service]
        [donkey.util])
  (:require [donkey.util.config :as config]))

(defn secured-coge-routes
  []
  (optional-routes
    [config/coge-enabled]
    (POST "/coge/load-genomes" [:as {body :body}]
          (trap #(get-genome-viewer-url body)))))

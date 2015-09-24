(ns donkey.services.coge
  (:use [donkey.auth.user-attributes]
        [donkey.util.service :only [decode-json prepare-forwarded-request success-response]])
  (:require [clojure.tools.logging :as log]
            [donkey.clients.coge :as coge]
            [donkey.clients.data-info :as data]
            [donkey.util.config :as config]))

(defn- share-paths
  "Shares the given paths with the CoGe user so the genome viewer service can access them."
  [paths]
  (let [sharer      (:shortUsername current-user)
        share-withs [(config/coge-user)]
        perms       :write]
    (data/share sharer share-withs paths perms)))

(defn get-genome-viewer-url
  "Retrieves a genome viewer URL by sharing the given paths and sending a request to the CoGe
   service."
  [body]
  (let [paths (:paths (decode-json body))]
    (share-paths paths)
    {:coge_genome_url (:site_url (coge/get-genome-viewer-url paths))}))

(defn search-genomes
  "Searches for genomes in CoGe."
  [{:keys [search]}]
  (coge/search-genomes search))

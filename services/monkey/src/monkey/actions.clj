(ns monkey.actions
  "This namespace contains the top-level tasks monkey performs. They are intended to be independent
   of the actual index, tags database and message queue implementations."
  (:require [clojure.tools.logging :as log]
            [monkey.index :as index]
            [monkey.props :as props]
            [monkey.tags :as tags])
  (:import [clojure.lang PersistentArrayMap]
           [monkey.index Indexes]
           [monkey.tags ViewsTags]))


(defn- all-tags
  [monkey]
  ;; TODO Log progress the way infosquito does
  (index/all-tags (:index monkey)))


(defn- remove-batch
  [monkey batch]
  (when (log/enabled? "trace")
    (doseq [tag batch]
      (log/trace "removing the document for tag" tag "from the search index")))
  (index/remove-tags (:index monkey) batch))


(defn- purge-missing-tags
  [monkey]
  (log/info "purging non-existent tags from search index")
  (let [props (:props monkey)]
    (->> (all-tags monkey)
      (partition-all (props/tags-batch-size props))
      (map (partial tags/filter-missing (:tags monkey)))
      lazy-cat
      (partition-all (props/es-batch-size props))
      (map (partial remove-batch monkey))
      dorun)))


(defn- reindex
  [])


(defn ^PersistentArrayMap mk-monkey
  "Creates a monkey. A monkey is the state-holding object used by the actions namespace.

   Parameters:
     props - the monkey configuration values
     index - the proxy for the search index that needs to be synchronized with the tags database.
     tags  - the proxy for the tags database

   Returns:
     It returns the monkey."
  [^PersistentArrayMap props
   ^Indexes            index
   ^ViewsTags          tags]
  {:props props
   :index index
   :tags  tags})


(defn sync-index
  "This function synchronizes the contents of the tag documents in the data search index with the
   tags information in the metadata database.

   Parameters:
     monkey - the monkey used"
  [^PersistentArrayMap monkey]
  (purge-missing-tags monkey)
  (reindex))
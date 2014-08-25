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


(defn- indexed-tags
  [monkey]
  ;; TODO Log progress the way infosquito does
  (index/all-tags (:index monkey)))


(defn- filter-missing-tags
  [monkey tag-ids]
  (->> (partition-all (props/tags-batch-size (:props monkey)) tag-ids)
    (map (partial tags/filter-missing (:tags monkey)))
    lazy-cat))


(defn- remove-tag-batch
  [monkey batch]
  (when (log/enabled? "trace")
    (doseq [tag batch]
      (log/trace "removing the document for tag" tag "from the search index")))
  (index/remove-tags (:index monkey) batch))


(defn- remove-from-index
  [monkey tag-ids]
  (doseq [batch (partition-all (props/es-batch-size (:props monkey)) tag-ids)]
    (remove-tag-batch monkey batch)))


(defn- purge-missing-tags
  [monkey]
  (log/info "purging non-existent tags from search index")
  ;; TODO indicate how tags will be inspected
  (->> (indexed-tags monkey)
    (filter-missing-tags monkey)
    (remove-from-index monkey)))


(defn- format-tag-doc
  [db-tag]
  (letfn [(format-target [tgt] {:id (:target_id tgt) :type (:target_type tgt)})]
    {:id           (:id db-tag)
     :value        (:value db-tag)
     :description  (:description db-tag)
     :creator      (:owner_id db-tag)
     :dateCreated  (:created_on db-tag)
     :dateModified (:modified_on db-tag)
     :targets      [(map format-target (:targets db-tag))]}))


(defn- index-tags
  [monkey tag-docs]
  ;; TODO Log progress the way infosquito does
  (doseq [batch (partition-all (props/es-batch-size (:props monkey)) tag-docs)]
    (index/index-tags (:index monkey) batch)))


(defn- reindex
  [monkey]
  (log/info "reindexing tags into the search index")
  (log/info "approximately" (tags/count-tags (:tags monkey)) "will be reindex")
  (->> (tags/all-tags (:tags monkey))
    (map format-tag-doc)
    (index-tags monkey)))


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
  (reindex monkey))
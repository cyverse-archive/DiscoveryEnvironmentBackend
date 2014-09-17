(ns monkey.actions
  "This namespace contains the top-level tasks monkey performs. They are intended to be independent
   of the actual index, tags database and message queue implementations."
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log]
            [clojure-commons.progress :as progress]
            [monkey.index :as index]
            [monkey.props :as props]
            [monkey.tags :as tags])
  (:import [clojure.lang PersistentArrayMap]
           [monkey.index Indexes]
           [monkey.tags ViewsTags]))


(defn- mk-prog-notifier
  [monkey]
  (fn [tags]
    (let [props       (:props monkey)
          prog-interval (props/progress-logging-interval props)]
      (mapcat (progress/notifier (props/log-progress? props)
                #(log/info %)
                prog-interval)
        (partition-all prog-interval tags)))))


(defn- filter-missing-tags
  [monkey tag-ids]
  (letfn [(filter-missing [batch]
            (set/difference (set batch)
                            (set (tags/remove-missing (:tags monkey) batch))))]
    (mapcat filter-missing
            (partition-all (props/tags-batch-size (:props monkey)) tag-ids))))


(defn- remove-tag-batch
  [monkey batch]
  (when (log/enabled? :trace)
    (doseq [tag batch]
      (log/trace "removing the document for tag" (str tag) "from the search index")))
  (index/remove-tags (:index monkey) batch))


(defn- remove-from-index
  [monkey tag-ids]
  (doseq [batch (partition-all (props/es-batch-size (:props monkey)) tag-ids)]
    (remove-tag-batch monkey batch)))


(defn- purge-missing-tags
  [monkey]
  (let [notify-prog (mk-prog-notifier monkey)]
    (log/info "purging non-existent tags from search index")
    (log/info "approximately" (index/count-tags (:index monkey)) "tag documents will be inspected")
    (->> (index/all-tags (:index monkey))
      notify-prog
      (filter-missing-tags monkey)
      (remove-from-index monkey))
    (log/info "finished purging non-existent tags from search index")))


(defn- attach-targets
  [monkey tags]
  (letfn [(attach [tag] (assoc tag :targets (tags/tag-targets (:tags monkey) (:id tag))))]
    (map attach tags)))


(defn- format-tag-docs
  [tags]
  (letfn [(format-target [target] {:id   (:target_id target)
                                   :type (str (:target_type target))})]
    (for [tag tags]
      {:id           (:id tag)
       :value        (:value tag)
       :description  (:description tag)
       :creator      (str (:owner_id tag) "#iplant")
       :dateCreated  (:created_on tag)
       :dateModified (:modified_on tag)
       :targets      (map format-target (:targets tag))})))


(defn- index-tags
  [monkey tag-docs]
  (letfn [(index [batch]
            (index/index-tags (:index monkey) batch)
            batch)]
    (mapcat index (partition-all (props/es-batch-size (:props monkey)) tag-docs))))


(defn- reindex
  [monkey]
  (log/info "reindexing tags into the search index")
  (log/info "approximately" (tags/count-tags (:tags monkey)) "will be reindexed")
  (tags/->>all-tags (:tags monkey)
    [(partial attach-targets monkey)
     format-tag-docs
     (partial index-tags monkey)
     (mk-prog-notifier monkey)])
  (log/info "finished reindexing tags into the search index"))


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
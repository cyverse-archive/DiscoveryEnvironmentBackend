(ns infosquito.es-crawler
  (:use [infosquito.progress :only [notifier]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.bulk :as bulk]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.response :as resp]
            [clojurewerkz.elastisch.query :as q]
            [infosquito.icat :as icat]
            [infosquito.index :as index]
            [infosquito.props :as cfg]))

(def ^:private index "data")

(defn- seed-item-seq
  [item-type props]
  ; The scan search type does not return any results with its first call, unlike all the other
  ; search types. A second call is needed to kick off the sequence.
  (let [res (esd/search index (name item-type)
              :query       (q/match-all)
              :fields      ["_id"]
              :search_type "scan"
              :scroll      "1m"
              :size        (cfg/get-es-scroll-size props))]
    (if (resp/any-hits? res)
      (esd/scroll (:_scroll_id res) :scroll "1m")
      res)))

(defn- item-seq
  [item-type props]
  (esd/scroll-seq (seed-item-seq item-type props)))

(defn- log-deletion
  [item-type item]
  (log/trace "deleting index entry for" (name item-type) (:_id item)))

(defn- log-failure
  [item-type item]
  (log/trace "unable to remove the index entry for" (name item-type) (:_id item)))

(defn- log-failures
  [res]
  (->> (:items res)
       (map :delete)
       (filter (complement resp/ok?))
       (map (fn [{id :_id type :_type}] (log-failure type id)))
       (dorun)))

(defn- delete-items
  [item-type items]
  (dorun (map (partial log-deletion item-type) items))
  (try
    (let [req (bulk/bulk-delete items)
          res (bulk/bulk-with-index-and-type index (:name item-type) req :refresh true)]
      (log-failures res))
    (catch Throwable t
      (dorun (map (partial log-failure (name item-type)) (map :id items))))))

(defn- retention-logger
  [item-type keep-item?]
  (fn [id]
    (let [keep? (keep-item? id)]
      (log/trace (name item-type) id (if keep? "exists" "does not exist"))
      keep?)))

(defn- purge-deleted-items
  [item-type keep? props]
  (log/info "purging non-existent" (name item-type) "entries")
  (->> (item-seq item-type props)
       (mapcat (comp (notifier (cfg/notify-enabled? props) (cfg/get-notify-count props)) vector))
       (remove (comp (retention-logger item-type keep?) :_id))
       (partition-all (cfg/get-index-batch-size props))
       (map (partial delete-items item-type))
       (dorun))
  (log/info (name item-type) "entry purging complete"))

(def ^:private purge-deleted-files (partial purge-deleted-items :file icat/file-exists?))

(defn- purge-deleted-folders
  [props]
  (let [index-base (cfg/get-base-collection props)]
    (purge-deleted-items :folder
                         #(and (index/indexable? index-base %) (icat/folder-exists? %))
                         props)))

(defn purge-index
  [props]
  (esr/connect! (cfg/get-es-url props))
  (purge-deleted-files props)
  (purge-deleted-folders props))

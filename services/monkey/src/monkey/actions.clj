(ns monkey.actions
  "This namespace contains the top-level tasks monkey performs. They are intended to be independent
   of the actual index, tags database and message queue implementations."
  (:import [monkey.index Indexes]
           [monkey.tags ViewsTags]))


(defn- purge-deleted
  [])


(defn- reindex
  [])


(defn sync-index
  "This function synchronizes the contents of the tag documents in the data search index with the
   tags information in the metadata database.

   Parameters:
     index - the object that interacts with the search index
     tags  - the object that interacts with the tags database"
  [^Indexes index ^ViewsTags tags]
  (purge-deleted)
  (reindex))
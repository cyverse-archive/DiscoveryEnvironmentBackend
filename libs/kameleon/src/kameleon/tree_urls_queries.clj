(ns kameleon.tree-urls-queries
  (:use [kameleon.core]
        [kameleon.uuids]
        [kameleon.misc-queries]
        [korma.core])
  (:require [kameleon.entities :as e]))

(defn tree-urls-seq
  "Returns all of the tree-url records associated with the given UUID."
  [uuid]
  (select e/tree-urls
          (where {:id (uuidify uuid)})))

(defn tree-urls?
  "Returns true if the UUID has tree-urls associated with it in the
   database."
  [uuid]
  (pos? (count (tree-urls-seq uuid))))

(defn tree-urls
  "Returns the tree-urls (as a string) associated with the UUID."
  [uuid]
  (-> (tree-urls-seq uuid) first :tree_urls))

(defn insert-tree-urls
  "Inserts tree urls into the database without checking to see if
   the UUID already exists. Use (save-tree-urls) instead."
  [uuid tree-urls-str]
  (insert e/tree-urls
          (values {:id (uuidify uuid) :tree_urls tree-urls-str})))

(defn save-tree-urls
  "Upserts tree-urls into the database."
  [uuid tree-urls-str]
  (if-not (tree-urls? uuid)
    (insert-tree-urls uuid tree-urls-str)
    (update e/tree-urls
            (set-fields {:tree_urls tree-urls-str})
            (where {:id (uuidify uuid)}))))

(defn delete-tree-urls
  "Does a hard delete of the tree-urls associated with UUID."
  [uuid]
  (delete e/tree-urls
          (where {:id (uuidify uuid)})))

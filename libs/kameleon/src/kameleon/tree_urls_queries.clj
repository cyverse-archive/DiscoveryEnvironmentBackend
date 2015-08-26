(ns kameleon.tree-urls-queries
  (:use [kameleon.core]
        [kameleon.uuids]
        [kameleon.misc-queries]
        [korma.core])
  (:require [kameleon.entities :as e]))

(defn tree-urls-seq
  "Returns all of the tree-url records associated with the given SHA1."
  [sha1]
  (select e/tree-urls
          (where {:sha1 sha1})))

(defn tree-urls?
  "Returns true if the SHA1 has tree-urls associated with it in the
   database."
  [sha1]
  (pos? (count (tree-urls-seq sha1))))

(defn tree-urls
  "Returns the tree-urls (as a string) associated with the SHA1."
  [sha1]
  (-> (tree-urls-seq sha1) first :tree_urls))

(defn insert-tree-urls
  "Inserts tree urls into the database without checking to see if
   the SHA1 already exists. Use (save-tree-urls) instead."
  [sha1 tree-urls-str]
  (insert e/tree-urls
          (values {:id (uuid) :sha1 sha1 :tree_urls tree-urls-str})))

(defn save-tree-urls
  "Upserts tree-urls into the database."
  [sha1 tree-urls-str]
  (if-not (tree-urls? sha1)
    (insert-tree-urls sha1 tree-urls-str)
    (do
      (update e/tree-urls
              (set-fields {:tree_urls tree-urls-str})
              (where {:sha1 sha1}))
      (first (select e/tree-urls
                     (where {:sha1 sha1}))))))

(defn delete-tree-urls
  "Does a hard delete of the tree-urls associated with UUID."
  [sha1]
  (delete e/tree-urls
          (where {:sha1 sha1})))

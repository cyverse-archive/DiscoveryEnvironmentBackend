(ns donkey.util.nibblonian
  (:require [donkey.services.filesystem.metadata :as mt]))


(defn format-tree-url
  "Creates a tree URL element."
  [label url]
  {:label label
   :url   url})

(defn format-tree-urls
  "Formats the tree URLs for storage in the file metadata.  The urls argument
   should contain a sequence of elements as returned by format-tree-url."
  [urls]
  {:tree-urls urls})

(defn save-tree-metaurl
  "Saves the URL used to get saved tree URLs.  The metaurl argument should
   contain the URL used to obtain the tree URLs."
  [path metaurl]
  (mt/admin-metadata-set path {:attr "tree-urls" :value metaurl :unit ""}))

(defn get-tree-metaurl
  "Gets the URL used to get saved tree URLs."
  [user path]
  (->> (mt/metadata-get user path)
    (:metadata)
    (filter #(= (:attr %) "tree-urls"))
    (first)
    (:value)))

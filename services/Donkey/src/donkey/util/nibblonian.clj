(ns donkey.util.nibblonian
  (:use [clojure-commons.error-codes])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure-commons.client :as cc]
            [donkey.services.filesystem.metadata :as mt]
            [donkey.services.filesystem.users :as u]))

(defn get-avus
  "Retrieves the AVUs associated with a file."
  [user path]
  (:metadata (mt/metadata-get user path)))

(defn avu-exists?
  "Determines if an AVU is associated with a file."
  [user path attr]
  (let [avus (get-avus user path)]
    (first (filter #(= (:attr %) attr) avus))))

(defn delete-avu
  "Removes an AVU from a file."
  [user path attr]
  (when (avu-exists? user path attr)
    (mt/metadata-delete user path attr)))

(defn delete-tree-urls
  "Removes all of the tree URLs associated with a file."
  [user path]
  (delete-avu user path "tree-urls"))

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
    (:filter #(= (:attr %) "tree-urls"))
    (first)
    (:value)))

(defn get-user-groups
  "Retrieves the set of groups a user belongs to."
  [user]
  (set (u/list-user-groups user)))

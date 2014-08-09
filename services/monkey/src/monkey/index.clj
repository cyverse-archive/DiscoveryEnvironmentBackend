(ns monkey.index
  "This namespace implements the Indexes protocol where elastisch library is used to interface with
   the search index."
  (:gen-class)
  (:require [clojurewerkz.elastisch.query :as query]
            [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.document :as doc]
            [clojurewerkz.elastisch.rest.response :as resp]
            [monkey.props :as props])
  (:import [clojure.lang ISeq PersistentArrayMap]))


(defn- init-tag-seq
  [props es]
  (let [res (doc/search es "data" "tags"
              :query       (query/match-all)
              :fields      ["_id"]
              :search_type "scan"
              :scroll      (props/es-scroll-timeout props)
              :size        (props/es-scroll-size props))]
    (if (resp/any-hits? res)
      (doc/scroll es (:_scroll_id res) :scroll "1m")
      res)))


(defprotocol Indexes
  "This protocol defines the operations needed to interact with the data search index."

  (^ISeq all-tags [_]
    "returns a sequence of all of the ids for the tag documents in the search index")

  (remove-tags [_ ^ISeq ids]
    "Removes the tags with the provided ids from the search index"))


(deftype ^{:private true} Index [props es]
  Indexes

  (all-tags [_]
    (map :_id (doc/scroll-seq es (init-tag-seq props es))))


  (remove-tags [_ ids]
    ;; TODO implement
    ))


(defn ^Indexes mk-index
  "creates the object used to interact with the search index

   Parameters:
     props - The configuration properties map

   Returns:
     It returns the object."
  [^PersistentArrayMap props]
  (->Index props
           (es/connect (str (props/es-url props)))))

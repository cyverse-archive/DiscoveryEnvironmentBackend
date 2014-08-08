(ns monkey.index
  "This namespace implements the Indexes protocol where elastisch library is used to interface with
   the search index."
  (:require [clojurewerkz.elastisch.rest :as es]
            [monkey.props :as props]))


(defprotocol Indexes
  "This protocol defines the operations needed to interact with the data search index.")


(deftype ^{:private true} Index [es]
  Indexes)


(defn ^Indexes mk-index
  "creates the object used to interact with the search index

   Parameters:
     props - The configuration properties map

   Returns:
     It returns the object."
  [props]
  (->Index (es/connect (str (props/es-url props)))))

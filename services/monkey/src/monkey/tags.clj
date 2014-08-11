(ns monkey.tags
  "This namespace implements the ViewTags protocol for interacting with the tags data store through
   Korma."
  (:gen-class)
  (:require [korma.db :as db]
            [monkey.props :as props])
  (:import [java.util UUID]
           [clojure.lang ISeq PersistentArrayMap]))


(defprotocol ViewsTags
  "This protocol defines the read-only operations needed to interact with the tag database."

  (^ISeq all-tags [_]
    "Retrieves the list of all of the tags in the database.")

  (^ISeq filter-missing [_ ^Iseq ids]
    "Indicates whether or not a tag with the given id is in the database."))


(deftype ^{:private true} Tags [db]
  ViewsTags

  (all-tags [_]
    ;; TODO implement
    [])

  (filter-missing [_ ids]
    ;; TODO implement
    []))


(defn ^ViewsTags mk-tags
  [^PersistentArrayMap props]
  (->Tags (db/postgres {:host     (props/tags-host props)
                        :port     (props/tags-port props)
                        :db       (props/tags-db props)
                        :user     (props/tags-user props)
                        :password (props/tags-password props)})))

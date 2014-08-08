(ns monkey.tags
  "This namespace implements the ViewTags protocol for interacting with the tags data store through
   Korma."
  (:require [korma.db :as db]
            [monkey.props :as props]))


(defprotocol ViewsTags
  "This protocol defines the read-only operations needed to interact with the tag database.")


(deftype ^{:private true} Tags [db]
  ViewsTags)


(defn ^ViewsTags mk-tags
  [props]
  (->Tags (db/postgres {:host     (props/tags-host props)
                        :port     (props/tags-port props)
                        :db       (props/tags-db props)
                        :user     (props/tags-user props)
                        :password (props/tags-password props)})))

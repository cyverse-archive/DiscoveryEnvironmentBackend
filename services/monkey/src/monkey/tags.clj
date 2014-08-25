(ns monkey.tags
  "This namespace implements the ViewTags protocol for interacting with the tags data store through
   Korma."
  (:gen-class)
  (:use korma.core)
  (:require [korma.db :as db]
            [monkey.props :as props])
  (:import [java.util UUID]
           [clojure.lang ISeq PersistentArrayMap]))


(defentity ^{:private true} target
  (table (subselect :attached_tags
           (where {:target_type [in [(raw "'file'") (raw "'folder'")]]
                   :detached_on nil}))
         :targets)
  (entity-fields :target_id :target_type))


(defentity ^{:private true} tag
  (table :tags)
  (entity-fields :id :value :description :owner_id :created_on :modified_on)
  (has-many target {:fk :tag_id}))


(defprotocol ViewsTags
  "This protocol defines the read-only operations needed to interact with the tag database."

  (^ISeq all-tags [_]
    "Retrieves the list of all of the tags in the database.")

  (^Integer count-tags [_]
    "Retreives a count of the number of tags in the database.")

  (^ISeq filter-missing [_ ^Iseq ids]
    "Indicates whether or not a tag with the given id is in the database."))


(deftype ^{:private true} Tags [db]
  ViewsTags

  (all-tags [_]
    (db/with-db db
      (select tag (with-batch target))))

  (count-tags [_]
    (-> (db/with-db db
          (select tag
            (aggregate (count :*) :cnt)))
      first :cnt))

  (filter-missing [_ ids]
    (db/with-db db
      (select tag
        (fields :id)
        (where {:id [in ids]})))))


(defn ^ViewsTags mk-tags
  [^PersistentArrayMap props]
  (->Tags (db/postgres {:host     (props/tags-host props)
                        :port     (props/tags-port props)
                        :db       (props/tags-db props)
                        :user     (props/tags-user props)
                        :password (props/tags-password props)})))

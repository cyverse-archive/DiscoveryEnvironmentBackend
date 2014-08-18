(ns monkey.props
  "This namespace holds all of the logic for managing configuration values."
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log])
  (:import [java.net URL]
           [clojure.lang PersistentArrayMap]))


(def ^{:private true :const true} prop-names
  #{"monkey.es.url"
    "monkey.es.index"
    "monkey.es.tag-type"
    "monkey.es.batch-size"
    "monkey.es.scroll-size"
    "monkey.es.scroll-timeout"
    "monkey.tags.host"
    "monkey.tags.port"
    "monkey.tags.db"
    "monkey.tags.user"
    "monkey.tags.password"
    "monkey.tags.batch-size"})


(defn ^Integer es-batch-size
  "Returns the indexing bulk operations batch size.

   Parameters:
     props - the property map to use

   Returns:
     the number of documents to handle at once in a bulk indexing operation"
  [^PersistentArrayMap props]
  (Integer/parseInt (get props "monkey.es.batch-size")))


(defn ^URL es-url
  "Returns the elasticsearch base URL.

   Parameters:
     props - The property map to use.

   Returns:
     It returns the elasticsearch base URL."
  [^PersistentArrayMap props]
  (URL. (get props "monkey.es.url")))


(defn ^String es-index
  "Returns the index in elasticsearch where the tags are indexed.

   Parameters:
     props - the property map to use

   Returns:
     the name of the index"
  [^PersistentArrayMap props]
  (get props "monkey.es.index"))


(defn ^String es-tag-type
  "returns the elasticsearch mapping type for a tag

   Parameters:
     props - the property map to use

   Returns:
     the tag mapping type"
  [^PersistentArrayMap props]
  (get props "monkey.es.tag-type"))


(defn ^Integer es-scroll-size
  "Returns the number of documents to retrieve at a time when scrolling through an elasticsearch
   result set.

   Parameters:
     props - the property map to use

   Returns:
     It returns the scroll size"
  [^PersistentArrayMap props]
  (Integer/parseInt (get props "monkey.es.scroll-size")))


(defn ^String es-scroll-timeout
  "Returns the unitted timeout value for a scroll.

   Parameters:
     props - the property map to use

   Returns:
     It returns the scroll timeout"
  [^PersistentArrayMap props]
  (get props "monkey.es.scroll-timeout"))


(defn ^String tags-host
  "Returns the tags database host

   Parameters:
     props - The property map to use.

   Returns:
     It returns the domain name of the host of the tags database."
  [^PersistentArrayMap props]
  (get props "monkey.tags.host"))


(defn ^Integer tags-port
  "Returns the tags database port

   Parameters:
     props - The property map to use.

   Returns:
     It returns the port the tags database listens on."
  [^PersistentArrayMap props]
  (Integer/parseInt (get props "monkey.tags.port")))


(defn ^String tags-db
  "Returns the name of the tags database

   Parameters:
     props - the property map to use

   Returns:
     It returns the name of the tags database"
  [^PersistentArrayMap props]
  (get props "monkey.tags.db"))


(defn ^String tags-user
  "Returns the username authorized to access the tags database.

   Parameters:
     props - The properties map to use.

   Returns:
     It returns the authorized username."
   [^PersistentArrayMap props]
  (get props "monkey.tags.user"))


(defn ^String tags-password
  "returns the password used to authenticate the authorized user

   Parameters:
     props - the property map to use

   Returns:
     It returns the password"
  [^PersistentArrayMap props]
  (get props "monkey.tags.password"))


(defn ^Integer tags-batch-size
  "Returns the tags inspection bulk operations batch size.

   Parameters:
     props - the property map to use

   Returns:
     the number of tags to handle at once in a bulk inspection operation"
  [^PersistentArrayMap props]
  (Integer/parseInt (get props "monkey.tags.batch-size")))


(defn ^Boolean validate
  "Validates the configuration. We don't want short-circuit evaluation in this case because
   logging all missing configuration settings is helpful.

   Parameters:
     props - The property map to validate

   Returns:
     It returns true if all of the required parameters are present and false otherwise."
  [^PersistentArrayMap props]
  (let [missing-props (set/difference prop-names (set (keys props)))
        all-present   (empty? missing-props)]
    (when-not all-present
      (doseq [missing-prop missing-props]
        (log/error "configuration setting" missing-prop "is undefined")))
    all-present))

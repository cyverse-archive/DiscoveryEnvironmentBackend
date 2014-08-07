(ns monkey.props
  "This namespace holds all of the logic for managing configuration values."
  (:require [clojure.set :as set])
  (:import [java.net URL]))


(def ^{:private true :const true} prop-names
  #{"monkey.es.url"
    "monkey.tags.host"
    "monkey.tags.port"
    "monkey.tags.db"
    "monkey.tags.user"
    "monkey.tags.password"})


(defn es-url
  "Returns the elasticsearch base URL.

   Parameters:
     props - The property map to use.

   Returns:
     It returns the elasticsearch base URL."
  [props]
  (URL. (get props "monkey.es.url")))


(defn tags-host
  "Returns the tags database host

   Parameters:
     props - The property map to use.

   Returns:
     It returns the domain name of the host of the tags database."
  [props]
  (get props "monkey.tags.host"))


(defn tags-port
  "Returns the tags database port

   Parameters:
     props - The property map to use.

   Returns:
     It returns the port the tags database listens on."
  [props]
  (Integer/parseInt (get props "monkey.tags.port")))


(defn tags-db
  "Returns the name of the tags database

   Parameters:
     props - the property map to use

   Returns:
     It returns the name of the tags database"
  [props]
  (get props "monkey.tags.db"))


(defn tags-user
  "Returns the username authorized to access the tags database.

   Parameters:
     props - The properties map to use.

   Returns:
     It returns the authorized username."
   [props]
  (get props "monkey.tags.user"))


(defn tags-password
  "returns the password used to authenticate the authorized user

   Parameters:
     props - the property map to use

   Returns:
     It returns the password"
  [props]
  (get props "monkey.tags.password"))


(defn validate
  "Validates the configuration. We don't want short-circuit evaluation in this case because
   logging all missing configuration settings is helpful.

   Parameters:
     props       - The property map to validate
     log-missing - The function used to log invalid properties. It accepts the name of the missing
                   property as its only argument.

   Returns:
     It returns true if all of the required parameters are present and false otherwise."
  [props log-missing]
  (let [missing-props (set/difference prop-names (set (keys props)))
        all-present   (empty? missing-props)]
    (when-not all-present
      (doseq [missing-prop missing-props]
        (log-missing missing-prop)))
    all-present))

(ns donkey.util.db
  (:use [donkey.util.config]
        [donkey.util.time :only [millis-from-str]]
        [korma.db])
  (:require [clojure.string :as string])
  (:import [java.sql Timestamp]))

(defn- create-db-spec
  "Creates the database connection spec to use when accessing the database
   using Korma."
  []
  {:classname   (db-driver-class)
   :subprotocol (db-subprotocol)
   :subname     (str "//" (db-host) ":" (db-port) "/" (db-name))
   :user        (db-user)
   :password    (db-password)})

(defn define-database
  "Defines the database connection to use from within Clojure."
  []
  (let [spec (create-db-spec)]
    (defonce de (create-db spec))
    (default-connection de)))

(defn timestamp-from-millis
  "Converts the number of milliseconds since the epoch to a timestamp."
  [millis]
  (when-not (nil? millis)
    (Timestamp. millis)))

(defn timestamp-from-str
  "Parses a string representation of a timestamp."
  [s]
  (timestamp-from-millis (millis-from-str s)))

(defn millis-from-timestamp
  "Converts a timestamp to the number of milliseconds since the epoch."
  [timestamp]
  (when-not (nil? timestamp)
    (.getTime timestamp)))

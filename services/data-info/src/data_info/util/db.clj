(ns data-info.util.db
  (:use [data-info.util.config]
        [data-info.util.time :only [millis-from-str]]
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

(defn- create-metadata-db-spec
  "Creates the database connection spec to use when accessing the metadata database using Korma."
  []
  {:classname   (metadata-db-driver-class)
   :subprotocol (metadata-db-subprotocol)
   :subname     (str "//" (metadata-db-host) ":" (metadata-db-port) "/" (metadata-db-name))
   :user        (metadata-db-user)
   :password    (metadata-db-password)})

(defn define-database
  "Defines the database connection to use from within Clojure."
  []
  (let [spec (create-db-spec)
        metadata-spec (create-metadata-db-spec)]
    (defonce de (create-db spec))
    (defonce metadata (create-db metadata-spec))
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

(defn now
  "Returns a timestamp representing the current date and time."
  []
  (Timestamp. (System/currentTimeMillis)))

(defn timestamp-str
  "Returns a string containing the number of milliseconds since the epoch for a timestamp."
  [timestamp]
  (when-not (nil? timestamp)
    (str (.getTime timestamp))))

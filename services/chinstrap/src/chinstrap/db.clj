(ns chinstrap.db
  (:use [chinstrap.config]
        [korma.db]
        [clojure.java.io :only [file]])
  (:import [com.mongodb MongoOptions ServerAddress])
  (:require [clojure.tools.logging :as log]
            [monger.core :as mg]
            [clojure-commons.props :as cp]))

(def mongo-db (ref nil))

(defn postgresdb-spec
  "Constructs a database connection specification from the configuration
   settings."
  []
  {:classname (postgresdb-driver)
   :subprotocol (postgresdb-subprotocol)
   :subname (str "//" (postgresdb-host) ":" (postgresdb-port) "/" (postgresdb-database))
   :user (postgresdb-user)
   :password (postgresdb-password)
   :max-idle-time (postgresdb-max-idle-time)})

(defn korma-define
  "Defines a korma representation of the database."
  []
  (let [spec (postgresdb-spec)]
    (defonce de (create-db spec))
    (default-connection de)))

(defn- build-mongo-options
  []
  (mg/mongo-options
   {:connections-per-host (mongodb-connections-per-host)
    :max-wait-time (mongodb-max-wait-time)
    :connect-timeout (mongodb-connect-timeout)
    :socket-timeout (mongodb-socket-timeout)
    :auto-connect-retry (mongodb-auto-connect-retry)}))

(defn mongodb-connect
  "Connects to the mongoDB."
  []
  (let [^MongoOptions opts (build-mongo-options)
        ^ServerAddress sa  (mg/server-address (mongodb-host) (mongodb-port))
        conn               (mg/connect sa opts)
        db                 (mg/get-db conn (mongodb-database))]
    (dosync (ref-set mongo-db db))))

(defn db-config
  "Sets up a connection to the database."
  [config-file]
  (load-config-from-file config-file)
  (mongodb-connect)
  (korma-define))

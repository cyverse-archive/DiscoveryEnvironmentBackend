(ns chinstrap.db
    (:use [chinstrap.config]
          [korma.db]
          [clojure.java.io :only [file]])
    (:import [com.mongodb MongoOptions ServerAddress])
    (:require [clojure.tools.logging :as log]
              [monger.core :as mg ]
              [clojure-commons.clavin-client :as cl]
              [clojure-commons.props :as cp]))

(defn load-configuration-from-props
  "Loads the configuration from the local properties file."
  [passed-filename]
  (let [filename "chinstrap.properties"
        conf-dir (System/getenv "IPLANT_CONF_DIR")]
    (if (nil? conf-dir)
      (reset! props (cp/read-properties (file "resources/conf/test/" passed-filename)))
      (reset! props (cp/read-properties (file conf-dir filename)))))

  (log/warn "Configuration Data from local properties file:")
  (log-config @props)

  (when-not (configuration-valid)
    (log/warn "THE CONFIGURATION IS INVALID - EXITING NOW")
    (System/exit 1)))

(defn load-configuration-from-zookeeper
  "Loads the configuration properties from Zookeeper and falls back to a
  local file if Zookeeper is not running."
  []
  (cl/with-zk
    (zk-props)
    (when-not (cl/can-run?)
      (log/warn "THIS APPLICATION CANNOT RUN ON THIS MACHINE. SO SAYETH ZOOKEEPER.")
      (log/warn "THIS APPLICATION WILL NOT EXECUTE CORRECTLY.")
      (System/exit 1))
    (reset! props (cl/properties "chinstrap")))
  
  (log/warn "Configuration Data loaded from the Zookeeper server:")
  (log-config @props)

  (when-not (configuration-valid)
    (log/warn "THE CONFIGURATION IS INVALID - EXITING NOW")
    (System/exit 1)))

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
  "Defines a korma representation of the database using the settings passed in from zookeeper."
  []
  (let [spec (postgresdb-spec)]
    (defonce de (create-db spec))
    (default-connection de)))

(defn mongodb-connect
  "Connects to the mongoDB using the settings passed in from zookeeper to monger."
  []
  (let [^MongoOptions opts
          (mg/mongo-options
              :connections-per-host (mongodb-connections-per-host)
              :max-wait-time (mongodb-max-wait-time)
              :connect-timeout (mongodb-connect-timeout)
              :socket-timeout (mongodb-socket-timeout)
              :auto-connect-retry (mongodb-auto-connect-retry))
        ^ServerAddress sa
          (mg/server-address (mongodb-host) (mongodb-port))]
    (mg/connect! sa opts))
  (mg/set-db! (mg/get-db (mongodb-database))))

(defn db-config
  "Sets up a connection to the database using config data loaded from zookeeper into Monger and Korma."
  []
  (load-configuration-from-zookeeper)
  ;(load-configuration-from-props "devs.properties")
  (mongodb-connect)
  (korma-define))

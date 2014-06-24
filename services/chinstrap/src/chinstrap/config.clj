(ns chinstrap.config
  (:use [clojure.string :only (blank? split)])
  (:require [clojure-commons.props :as cc-props]
            [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]))

(def ^:private props
  "A ref for storing the configuration properties."
  (ref nil))

(def ^:private config-valid
  "A ref for storing a configuration validity flag."
  (ref true))

(def ^:private configs
  "A ref for storing the symbols used to get configuration settings."
  (ref []))

(cc/defprop-str postgresdb-driver
  "The database driver."
  [props config-valid configs]
  "chinstrap.postgresdb.driver")

(cc/defprop-str postgresdb-subprotocol
  "The database subprotocol."
  [props config-valid configs]
  "chinstrap.postgresdb.subprotocol")

(cc/defprop-str postgresdb-host
  "the host name or IP address used to connect to the database."
  [props config-valid configs]
  "chinstrap.postgresdb.host")

(cc/defprop-int postgresdb-port
  "The port used to connect to the database."
  [props config-valid configs]
  "chinstrap.postgresdb.port")

(cc/defprop-str postgresdb-database
  "The name of the database."
  [props config-valid configs]
  "chinstrap.postgresdb.database")

(cc/defprop-str postgresdb-user
  "The database username."
  [props config-valid configs]
  "chinstrap.postgresdb.user")

(cc/defprop-str postgresdb-password
  "The database password."
  [props config-valid configs]
  "chinstrap.postgresdb.password")

(cc/defprop-str postgresdb-max-idle-time
  "The max idle time for the database in minutes."
  [props config-valid configs]
  "chinstrap.postgresdb.max-idle-time")

(cc/defprop-str mongodb-host
  "the host name or IP address used to connect to the database."
  [props config-valid configs]
  "chinstrap.mongodb.host")

(cc/defprop-int mongodb-port
  "The port used to connect to the database."
  [props config-valid configs]
  "chinstrap.mongodb.port")

(cc/defprop-str mongodb-database
  "The name of the database."
  [props config-valid configs]
  "chinstrap.mongodb.database")

(cc/defprop-int mongodb-connections-per-host
  "The max number of connections one host can have."
  [props config-valid configs]
  "chinstrap.mongodb.connections-per-host")

(cc/defprop-int mongodb-max-wait-time
  "The maximum wait time in milliseconds that a thread may wait for a connection to become
   available. A value of 0 means that it will not wait. A negative value means to wait
   indefinitely."
  [props config-valid configs]
  "chinstrap.mongodb.max-wait-time")

(cc/defprop-int mongodb-connect-timeout
  "Time it takes for the database connection to timeout in milliseconds"
  [props config-valid configs]
  "chinstrap.mongodb.connect-timeout")

(cc/defprop-int mongodb-socket-timeout
  "Time it takes for the socket connection to timeout in milliseconds"
  [props config-valid configs]
  "chinstrap.mongodb.socket-timeout")

(cc/defprop-boolean mongodb-auto-connect-retry
  "Whether or not the database should attempt to auto-connect"
  [props config-valid configs]
  "chinstrap.mongodb.auto-connect-retry")

(cc/defprop-str mongodb-bucket
  "The bucket in the database whose data will be accessed."
  [props config-valid configs]
  "chinstrap.mongodb.bucket")

(cc/defprop-int listen-port
  "The port to listen to for incoming connections."
  [props config-valid configs]
  "chinstrap.app.listen-port")

(defn- validate-config
  []
  (when-not (cc/validate-config configs config-valid)
    (log/warn "THE CONFIGURATION IS INVALID - EXITING NOW")
    (System/exit 1)))

(defn- exception-filters
  []
  (remove nil? [(postgresdb-password)]))

(defn load-config-from-file
  [path]
  (cc/load-config-from-file path props)
  (cc/log-config props :filters [#"irods\.user" #"icat\.user" #"oauth\.pem"])
  (validate-config)
  (ce/register-filters (exception-filters)))

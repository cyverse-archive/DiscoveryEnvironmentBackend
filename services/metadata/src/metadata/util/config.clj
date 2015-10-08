(ns metadata.util.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.config :as cc]
            [clojure.tools.logging :as log]))

(def default-config-file "/etc/iplant/de/metadata.properties")

(def docs-uri "/docs")

(def svc-info
  {:desc     "The REST API for the Discovery Environment Metadata services."
   :app-name "metadata"
   :group-id "org.iplantc"
   :art-id   "metadata"
   :service  "metadata"})

(def ^:private props
  "A ref for storing the configuration properties."
  (ref nil))

(def ^:private config-valid
  "A ref for storing a configuration validity flag."
  (ref true))

(def ^:private configs
  "A ref for storing the symbols used to get configuration settings."
  (ref []))

(cc/defprop-int listen-port
  "The port that metadata listens to."
  [props config-valid configs]
  "metadata.app.listen-port")

(cc/defprop-str environment-name
  "The name of the environment that this instance of Donkey belongs to."
  [props config-valid configs]
  "metadata.app.environment-name")

;;;Database connection information
(cc/defprop-str db-driver-class
  "The name of the JDBC driver to use."
  [props config-valid configs]
  "metadata.db.driver" )

(cc/defprop-str db-subprotocol
  "The subprotocol to use when connecting to the database (e.g. postgresql)."
  [props config-valid configs]
  "metadata.db.subprotocol")

(cc/defprop-str db-host
  "The host name or IP address to use when
   connecting to the database."
  [props config-valid configs]
  "metadata.db.host")

(cc/defprop-str db-port
  "The port number to use when connecting to the database."
  [props config-valid configs]
  "metadata.db.port")

(cc/defprop-str db-name
  "The name of the database to connect to."
  [props config-valid configs]
  "metadata.db.name")

(cc/defprop-str db-user
  "The username to use when authenticating to the database."
  [props config-valid configs]
  "metadata.db.user")

(cc/defprop-str db-password
  "The password to use when authenticating to the database."
  [props config-valid configs]
  "metadata.db.password")
;;;End database connection information

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:type :clojure-commons.exception/invalid-cfg})))

(defn log-environment
  []
  (log/warn "ENV? metadata.db.host =" (db-host)))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props)
  (log-environment)
  (validate-config))

(ns metadactyl.util.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [clojure.string :as str]))

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
  "The port that metadactyl listens to."
  [props config-valid configs]
  "metadactyl.app.listen-port")

(cc/defprop-str db-driver-class
  "The name of the JDBC driver to use."
  [props config-valid configs]
  "metadactyl.db.driver" )

(cc/defprop-str db-subprotocol
  "The subprotocol to use when connecting to the database (e.g.
   postgresql)."
  [props config-valid configs]
  "metadactyl.db.subprotocol")

(cc/defprop-str db-host
  "The host name or IP address to use when
   connecting to the database."
  [props config-valid configs]
  "metadactyl.db.host")

(cc/defprop-str db-port
  "The port number to use when connecting to the database."
  [props config-valid configs]
  "metadactyl.db.port")

(cc/defprop-str db-name
  "The name of the database to connect to."
  [props config-valid configs]
  "metadactyl.db.name")

(cc/defprop-str db-user
  "The username to use when authenticating to the database."
  [props config-valid configs]
  "metadactyl.db.user")

(cc/defprop-str db-password
  "The password to use when authenticating to the database."
  [props config-valid configs]
  "metadactyl.db.password")

(cc/defprop-vec hibernate-resources
  "The names of the hibernate resource files to include in the Hibernate
   session factory configuration."
  [props config-valid configs]
  "metadactyl.hibernate.resources")

(cc/defprop-vec hibernate-packages
  "The names of Java packages that Hibernate needs to scan for JPA
   annotations."
  [props config-valid configs]
  "metadactyl.hibernate.packages")

(cc/defprop-str hibernate-dialect
  "The dialect that Hibernate should use when generating SQL."
  [props config-valid configs]
  "metadactyl.hibernate.dialect")

(cc/defprop-str osm-base-url
  "The base URL to use when connecting to the OSM."
  [props config-valid configs]
  "metadactyl.osm.base-url")

(cc/defprop-int osm-connection-timeout
  "The maximum number of milliseconds to wait for a connection to the OSM."
  [props config-valid configs]
  "metadactyl.osm.connection-timeout")

(cc/defprop-str osm-encoding
  "The character encoding to use when communicating with the OSM."
  [props config-valid configs]
  "metadactyl.osm.encoding")

(cc/defprop-str osm-jobs-bucket
  "The OSM bucket containing information about jobs that the user has
   submitted."
  [props config-valid configs]
  "metadactyl.osm.jobs-bucket")

(cc/defprop-str osm-job-request-bucket
  "The OSM bucket containing job submission requests that were sent from the
   UI to metadactyl."
  [props config-valid configs]
  "metadactyl.osm.job-request-bucket")

(cc/defprop-str jex-base-url
  "The base URL to use when connecting to the JEX."
  [props config-valid configs]
  "metadactyl.jex.base-url")

(cc/defprop-str workspace-root-app-group
  "The name of the root app group in each user's workspace."
  [props config-valid configs]
  "metadactyl.workspace.root-app-group")

(cc/defprop-str workspace-default-app-groups
  "The names of the app groups that appear immediately beneath the root app
   group in each user's workspace."
  [props config-valid configs]
  "metadactyl.workspace.default-app-groups")

(cc/defprop-int workspace-dev-app-group-index
  "The index of the category within a user's workspace for apps under
   development."
  [props config-valid configs]
  "metadactyl.workspace.dev-app-group-index")

(cc/defprop-int workspace-favorites-app-group-index
  "The index of the category within a user's workspace for favorite apps."
  [props config-valid configs]
  "metadactyl.workspace.favorites-app-group-index")

(cc/defprop-str uid-domain
  "The domain name to append to the user identifier to get the fully qualified
   user identifier."
  [props config-valid configs]
  "metadactyl.uid.domain")

(cc/defprop-str irods-home
  "The path to the home directory in iRODS."
  [props config-valid configs]
  "metadactyl.irods.home")

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  []
  (cc/load-config-from-file (System/getenv "IPLANT_CONF_DIR") "metadactyl.properties" props)
  (cc/log-config props)
  (validate-config))

(defn load-config-from-zookeeper
  "Loads the configuration settings from Zookeeper."
  []
  (cc/load-config-from-zookeeper props "metadactyl")
  (cc/log-config props)
  (validate-config))

(def get-default-app-groups
  (memoize
  (fn []
    (cheshire/decode (str/replace (workspace-default-app-groups) #"\\," ",") true))))

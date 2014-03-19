(ns jex.config
  (:use [clojure-commons.props]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure-commons.clavin-client :as cl]
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

(cc/defprop-str filetool-path
  "Returns the path to porklock on the filesystem out on the Condor cluster."
  [props config-valid configs]
  "jex.app.filetool-path")

(cc/defprop-str nfs-base
  "Returns the path to the NFS directory on the submission host."
  [props config-valid configs]
  "jex.app.nfs-base")

(cc/defprop-str irods-base
  "Returns the path to the home directory in iRODS. Usually /iplant/home"
  [props config-valid configs]
  "jex.app.irods-base")

(cc/defprop-str irods-user
  "Returns the user that porklock should connect as."
  [props config-valid configs]
  "jex.app.irods-user")

(cc/defprop-str irods-pass
  "Returns the iRODS user's password."
  [props config-valid configs]
  "jex.app.irods-pass")

(cc/defprop-str irods-host
  "Returns the iRODS hostname/IP address."
  [props config-valid configs]
  "jex.app.irods-host")

(cc/defprop-str irods-port
  "Returns the iRODS port."
  [props config-valid configs]
  "jex.app.irods-port")

(cc/defprop-str irods-zone
  "Returns the iRODS zone."
  [props config-valid configs]
  "jex.app.irods-zone")

(cc/defprop-optstr irods-resc
  "Returns the iRODS resource."
  [props config-valid configs]
  "jex.app.irods-resc")

(cc/defprop-str icommands-path
  "Returns the path to the iRODS icommands out on the Condor cluster."
  [props config-valid configs]
  "jex.app.icommands-path")

(cc/defprop-str condor-log-path
  "Returns the path to the logs directory for Condor on the submission host."
  [props config-valid configs]
  "jex.app.condor-log-path")

(cc/defprop-int listen-port
  "Returns the port to accept requests on."
  [props config-valid configs]
  "jex.app.listen-port")

(cc/defprop-vec filter-files
  "A vector of filenames that should not be returned by porklock."
  [props config-valid configs]
  "jex.app.filter-files")

(cc/defprop-boolean run-on-nfs
  "Whether or not the JEX should run on NFS."
  [props config-valid configs]
  "jex.app.run-on-nfs")

(cc/defprop-str condor-config
  "The path to the condor_config file."
  [props config-valid configs]
  "jex.env.condor-config")

(cc/defprop-str path-env
  "The PATH environment variable value."
  [props config-valid configs]
  "jex.env.path")

(cc/defprop-str osm-url
  "The URL to the OSM."
  [props config-valid configs]
  "jex.osm.url")

(cc/defprop-str osm-coll
  "The collection to use in the OSM."
  [props config-valid configs]
  "jex.osm.collection")

(cc/defprop-str notif-url
  "The URL to the notification agent."
  [props config-valid configs]
  "jex.osm.notification-url")

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn- exception-filters
  []
  (mapv #(re-pattern (str %))
        [(irods-user) (irods-pass)]))

(defn register-exception-filters
  []
  (ce/register-filters (exception-filters)))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  []
  (cc/load-config-from-file (System/getenv "IPLANT_CONF_DIR") "jex.properties" props)
  (cc/log-config props :filters [#"irods\-user"])
  (validate-config))

(defn load-config-from-zookeeper
  "Loads the configuration settings from Zookeeper."
  []
  (cc/load-config-from-zookeeper props "jex")
  (cc/log-config props :filters [#"irods\-user"])
  (validate-config))

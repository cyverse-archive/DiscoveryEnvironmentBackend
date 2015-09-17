(ns porklock.config
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.file-utils :as cf]
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

(cc/defprop-str irods-home
  "Returns the path to the home directory in iRODS. Usually /iplant/home"
  [props config-valid configs]
  "porklock.irods-home")

(cc/defprop-str irods-user
  "Returns the user that porklock should connect as."
  [props config-valid configs]
  "porklock.irods-user")

(cc/defprop-str irods-pass
  "Returns the iRODS user's password."
  [props config-valid configs]
  "porklock.irods-pass")

(cc/defprop-str irods-host
  "Returns the iRODS hostname/IP address."
  [props config-valid configs]
  "porklock.irods-host")

(cc/defprop-str irods-port
  "Returns the iRODS port."
  [props config-valid configs]
  "porklock.irods-port")

(cc/defprop-str irods-zone
  "Returns the iRODS zone."
  [props config-valid configs]
  "porklock.irods-zone")

(cc/defprop-optstr irods-resc
  "Returns the iRODS resource."
  [props config-valid configs]
  "porklock.irods-resc")

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  [config-path]
  (cc/load-config-from-file (cf/dirname config-path) (cf/basename config-path) props)
  (cc/log-config props :filters [#"irods-user"])
  (validate-config))


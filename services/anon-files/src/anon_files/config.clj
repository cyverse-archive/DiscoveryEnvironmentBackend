(ns anon-files.config
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]))


(def ^:private props
  "A ref for storing the configuration properties."
  (ref nil))

(def ^:private config-valid
  "A ref for storing a configuration validity flag."
  (ref true))

(def ^:private configs
  "A ref for storing the symbols used to get configuration settings."
  (ref []))

(cc/defprop-str irods-user
  "The irods user"
  [props config-valid configs]
  "anon-files.irods-user")

(cc/defprop-str irods-pass
  "The irods password"
  [props config-valid configs]
  "anon-files.irods-password")

(cc/defprop-str irods-host
  "The irods hostname"
  [props config-valid configs]
  "anon-files.irods-host")

(cc/defprop-int irods-port
  "The irods port"
  [props config-valid configs]
  "anon-files.irods-port")

(cc/defprop-str irods-zone
  "The irods zone"
  [props config-valid configs]
  "anon-files.irods-zone")

(cc/defprop-str irods-home
  "The irods home directory"
  [props config-valid configs]
  "anon-files.irods-home")

(cc/defprop-int listen-port
  "The port to listen on"
  [props config-valid configs]
  "anon-files.listen-port")

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
  [filepath]
  (cc/load-config-from-file filepath props)
  (cc/log-config props :filters [#"irods\-user"])
  (validate-config))

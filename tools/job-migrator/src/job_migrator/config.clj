(ns job-migrator.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [cemerick.url :as curl]
            [clojure-commons.config :as cc]
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

(defn masked-config
  "Returns a masked version of the Donkey config as a map."
  []
  (cc/mask-config props :filters [#"(?:irods|agave)[-.](?:user|pass|key|secret)"]))

;;;Database connection information
(cc/defprop-str db-driver-class
  "The name of the JDBC driver to use."
  [props config-valid configs]
  "donkey.db.driver" )

(cc/defprop-str db-subprotocol
  "The subprotocol to use when connecting to the database (e.g.
   postgresql)."
  [props config-valid configs]
  "donkey.db.subprotocol")

(cc/defprop-str db-host
  "The host name or IP address to use when
   connecting to the database."
  [props config-valid configs]
  "donkey.db.host")

(cc/defprop-str db-port
  "The port number to use when connecting to the database."
  [props config-valid configs]
  "donkey.db.port")

(cc/defprop-str db-name
  "The name of the database to connect to."
  [props config-valid configs]
  "donkey.db.name")

(cc/defprop-str db-user
  "The username to use when authenticating to the database."
  [props config-valid configs]
  "donkey.db.user")

(cc/defprop-str db-password
  "The password to use when authenticating to the database."
  [props config-valid configs]
  "donkey.db.password")
;;;End database connection information

;;;OSM connection information
(cc/defprop-str osm-base-url
  "The base URL to use when connecting to the OSM."
  [props config-valid configs]
  "donkey.osm.base-url")

(cc/defprop-str osm-jobs-bucket
  "The OSM bucket containing information about jobs that the user has
   submitted."
  [props config-valid configs]
  "donkey.osm.jobs-bucket")

(cc/defprop-str osm-job-request-bucket
  "The OSM bucket containing information about job requests."
  [props config-valid configs]
  "donkey.osm.job-request-bucket")
;;;End OSM connection information

;;;Agave settings
(cc/defprop-str agave-base-url
  "The base URL to use when connecting to Agave."
  [props config-valid configs]
  "donkey.agave.base-url")

(cc/defprop-str agave-key
  "The API key to use when authenticating to Agave."
  [props config-valid configs]
  "donkey.agave.key")

(cc/defprop-str agave-secret
  "The API secret to use when authenticating to Agave."
  [props config-valid configs]
  "donkey.agave.secret")

(cc/defprop-str agave-oauth-base
  "The base URL for the Agave OAuth 2.0 endpoints."
  [props config-valid configs]
  "donkey.agave.oauth-base")

(cc/defprop-int agave-oauth-refresh-window
  "The number of minutes before a token expires to refresh it."
  [props config-valid configs]
  "donkey.agave.oauth-refresh-window")

(cc/defprop-str agave-redirect-uri
  "The redirect URI used after Agave authorization."
  [props config-valid configs]
  "donkey.agave.redirect-uri")

(cc/defprop-optstr agave-storage-system
  "The storage system that Agave should use when interacting with the DE."
  [props config-valid configs]
  "donkey.agave.storage-system"
  "data.iplantcollaborative.org")

(cc/defprop-optboolean agave-jobs-enabled
  "Enables or disables Agave job submission."
  [props config-valid configs]
  "donkey.features.agave.jobs" false)
;;;End Agave settings

;;;GPG settings
(cc/defprop-optstr keyring-path
  "The path to the secure PGP keyring."
  [props config-valid configs]
  "donkey.pgp.keyring-path"
  "/etc/iplant/de/crypto/secring.gpg")

(cc/defprop-str key-password
  "The password needed to unlock the PGP password."
  [props config-valid configs]
  "donkey.pgp.key-password")
;;;End GPG settings

(defn- oauth-settings
  [api-name api-key api-secret token-uri redirect-uri refresh-window]
  {:api-name       api-name
   :client-key     api-key
   :client-secret  api-secret
   :token-uri      token-uri
   :redirect-uri   redirect-uri
   :refresh-window (* refresh-window 60 1000)})

(def agave-oauth-settings
  (memoize
   #(oauth-settings
     "agave"
     (agave-key)
     (agave-secret)
     (str (curl/url (agave-oauth-base) "token"))
     (agave-redirect-uri)
     (agave-oauth-refresh-window))))

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (validate-config))

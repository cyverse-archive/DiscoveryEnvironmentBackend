(ns metadactyl.util.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [common-cfg.cfg :as cfg]))

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

(cc/defprop-optboolean agave-enabled
  "Enables or disables all features that require connections to Agave."
  [props config-valid configs]
  "metadactyl.features.agave" true)

(cc/defprop-optboolean agave-jobs-enabled
  "Enables or disables Agave job submission."
  [props config-valid configs]
  "metadactyl.features.agave.jobs" false)

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

(cc/defprop-str jex-base-url
  "The base URL to use when connecting to the JEX."
  [props config-valid configs]
  "metadactyl.jex.base-url")

(cc/defprop-str data-info-base-url
  "The base URL to use when connecting to the JEX."
  [props config-valid configs]
  "metadactyl.data-info.base-url")

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

(cc/defprop-str workspace-beta-app-category-id
  "The UUID of the default Beta app category."
  [props config-valid configs]
  "metadactyl.workspace.beta-app-category-id")

(cc/defprop-str workspace-public-id
  "The UUID of the default Beta app category."
  [props config-valid configs]
  "metadactyl.workspace.public-id")

(cc/defprop-str uid-domain
  "The domain name to append to the user identifier to get the fully qualified
   user identifier."
  [props config-valid configs]
  "metadactyl.uid.domain")

(cc/defprop-str irods-home
  "The path to the home directory in iRODS."
  [props config-valid configs]
  "metadactyl.irods.home")

(cc/defprop-str jex-batch-group-name
  "The group name to submit to the JEX for batch jobs."
  [props config-valid configs]
  "metadactyl.batch.group")

(cc/defprop-str path-list-info-type
  "The info type for HT Analysis Path Lists."
  [props config-valid configs]
  "metadactyl.batch.path-list.info-type")

(cc/defprop-int path-list-max-paths
  "The maximum number of paths to process per HT Analysis Path Lists."
  [props config-valid configs]
  "metadactyl.batch.path-list.max-paths")

(cc/defprop-int path-list-max-size
  "The maximum size of each HT Analysis Path List that can be fetched from the data-info service."
  [props config-valid configs]
  "metadactyl.batch.path-list.max-size")

(cc/defprop-str agave-base-url
  "The base URL to use when connecting to Agave."
  [props config-valid configs]
  "metadactyl.agave.base-url")

(cc/defprop-str agave-key
  "The API key to use when authenticating to Agave."
  [props config-valid configs]
  "metadactyl.agave.key")

(cc/defprop-str agave-secret
  "The API secret to use when authenticating to Agave."
  [props config-valid configs]
  "metadactyl.agave.secret")

(cc/defprop-str agave-oauth-base
  "The base URL for the Agave OAuth 2.0 endpoints."
  [props config-valid configs]
  "metadactyl.agave.oauth-base")

(cc/defprop-int agave-oauth-refresh-window
  "The number of minutes before a token expires to refresh it."
  [props config-valid configs]
  "metadactyl.agave.oauth-refresh-window")

(cc/defprop-str agave-redirect-uri
  "The redirect URI used after Agave authorization."
  [props config-valid configs]
  "metadactyl.agave.redirect-uri")

(cc/defprop-str agave-callback-base
  "The base URL for receiving job status update callbacks from Agave."
  [props config-valid configs]
  "metadactyl.agave.callback-base")

(cc/defprop-optstr agave-storage-system
  "The storage system that Agave should use when interacting with the DE."
  [props config-valid configs]
  "metadactyl.agave.storage-system"
  "data.iplantcollaborative.org")

(cc/defprop-str pgp-keyring-path
  "The path to the PGP keyring file."
  [props config-valid configs]
  "metadactyl.pgp.keyring-path")

(cc/defprop-str pgp-key-password
  "The password used to unlock the PGP key."
  [props config-valid configs]
  "metadactyl.pgp.key-password")

(def data-info-base
  (memoize
   (fn []
     (if (System/getenv "DATA_INFO_PORT")
       (cfg/env-setting "DATA_INFO_PORT")
       (data-info-base-url)))))

(defn- oauth-settings
  [api-name api-key api-secret auth-uri token-uri redirect-uri refresh-window]
  {:api-name       api-name
   :client-key     api-key
   :client-secret  api-secret
   :auth-uri       auth-uri
   :token-uri      token-uri
   :redirect-uri   redirect-uri
   :refresh-window (* refresh-window 60 1000)})

(def agave-oauth-settings
  (memoize
   #(oauth-settings
     "agave"
     (agave-key)
     (agave-secret)
     (str (curl/url (agave-oauth-base) "authorize"))
     (str (curl/url (agave-oauth-base) "token"))
     (agave-redirect-uri)
     (agave-oauth-refresh-window))))

(defn log-environment
  []
  (log/warn "ENV?: metadactyl.data-info.base-url = " (data-info-base)))

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props)
  (log-environment)
  (validate-config))

(def get-default-app-groups
  (memoize
  (fn []
    (cheshire/decode (str/replace (workspace-default-app-groups) #"\\," ",") true))))

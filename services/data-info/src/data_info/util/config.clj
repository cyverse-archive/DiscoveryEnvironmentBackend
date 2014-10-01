(ns data-info.util.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
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
  "Returns a masked version of the data-info config as a map."
  []
  (cc/mask-config props :filters [#"(?:irods|agave)[-.](?:user|pass|key|secret)"]))

(cc/defprop-int listen-port
  "The port that data-info listens to."
  [props config-valid configs]
  "data-info.app.listen-port")

(cc/defprop-str environment-name
  "The name of the environment that this instance of data-info belongs to."
  [props config-valid configs]
  "data-info.app.environment-name")

(cc/defprop-str cas-server
  "The base URL used to connect to the CAS server."
  [props config-valid configs]
  "data-info.cas.cas-server")

(cc/defprop-str pgt-callback-base
  "The base URL to use for proxy granting ticket callbacks from the CAS server."
  [props config-valid configs]
  "data-info.cas.pgt-callback-base")

(cc/defprop-str pgt-callback-path
  "The URL path to use for proxy granting ticket callbacks from the CAS server."
  [props config-valid configs]
  "data-info.cas.pgt-callback-path")

(cc/defprop-str server-name
  "The name of the local server."
  [props config-valid configs]
  "data-info.cas.server-name")

(cc/defprop-str uid-domain
  "The domain name to append to the user identifier to get the fully qualified
   user identifier."
  [props config-valid configs]
  "data-info.uid.domain")

(cc/defprop-optboolean admin-routes-enabled
  "Enables or disables the administration routes."
  [props config-valid configs]
  "data-info.routes.admin" true)

(cc/defprop-optboolean notification-routes-enabled
  "Enables or disables notification endpoints."
  [props config-valid configs]
  "data-info.routes.notifications" true)

(cc/defprop-optboolean app-routes-enabled
  "Enables or disables app endpoints."
  [props config-valid configs]
  "data-info.routes.apps" true)

(cc/defprop-optboolean metadata-routes-enabled
  "Enables or disables metadata endpoints."
  [props config-valid configs]
  "data-info.routes.metadata" true)

(cc/defprop-optboolean pref-routes-enabled
  "Enables or disables preferences endpoints."
  [props config-valid configs]
  "data-info.routes.prefs" true)

(cc/defprop-optboolean user-info-routes-enabled
  "Enables or disables user-info endpoints."
  [props config-valid configs]
  "data-info.routes.user-info" true)

(cc/defprop-optboolean data-routes-enabled
  "Enables or disables data endpoints."
  [props config-valid configs]
  "data-info.routes.data" true)

(cc/defprop-optboolean tree-viewer-routes-enabled
  "Enables or disables tree-viewer endpoints."
  [props config-valid configs]
  "data-info.routes.tree-viewer" true)

(cc/defprop-optboolean session-routes-enabled
  "Enables or disables user session endpoints."
  [props config-valid configs]
  "data-info.routes.session" true)

(cc/defprop-optboolean collaborator-routes-enabled
  "Enables or disables collaborator routes."
  [props config-valid configs]
  "data-info.routes.collaborator" true)

(cc/defprop-optboolean fileio-routes-enabled
  "Enables or disables the fileio routes."
  [props config-valid configs]
  "data-info.routes.fileio" true)

(cc/defprop-optboolean filesystem-routes-enabled
  "Enables or disables the filesystem routes."
  [props config-valid configs]
  "data-info.routes.filesystem" true)

(cc/defprop-optboolean search-routes-enabled
  "Enables or disables the search related routes."
  [props config-valid configs]
  "data-info.routes.search" false)

(cc/defprop-optboolean coge-enabled
  "Enables or disables COGE endpoints."
  [props config-valid configs]
  "data-info.routes.coge" true)

(cc/defprop-optboolean agave-enabled
  "Enables or disables all features that require connections to Agave."
  [props config-valid configs]
  "data-info.features.agave" true)

(cc/defprop-optboolean agave-jobs-enabled
  "Enables or disables Agave job submission."
  [props config-valid configs]
  "data-info.features.agave.jobs" false)

(cc/defprop-optboolean rabbitmq-enabled
  "Enables or disables RabbitMQ connection."
  [props config-valid configs]
  "data-info.features.rabbitmq" false)

(cc/defprop-optboolean log-runtimes
  "Enables or disables the logging of runtimes for endpoints that support it."
  [props config-valid configs]
  "data-info.debug.log-runtimes" false)

(cc/defprop-optboolean debug-ownership
  "Enables or disables the ownership check for folders in the home directory."
  [props config-valid configs]
  "data-info.debug.ownership" false)

(cc/defprop-str iplant-email-base-url
  "The base URL to use when connnecting to the iPlant email service."
  [props config-valid configs app-routes-enabled]
  "data-info.email.base-url")

(cc/defprop-str tool-request-dest-addr
  "The destination email address for tool request messages."
  [props config-valid configs app-routes-enabled]
  "data-info.email.tool-request-dest")

(cc/defprop-str tool-request-src-addr
  "The source email address for tool request messages."
  [props config-valid configs app-routes-enabled]
  "data-info.email.tool-request-src")

(cc/defprop-str feedback-dest-addr
  "The destination email address for DE feedback messages."
  [props config-valid configs app-routes-enabled]
  "data-info.email.feedback-dest")

(cc/defprop-str metadactyl-base-url
  "The base URL to use when connecting to secured Metadactyl services."
  [props config-valid configs app-routes-enabled]
  "data-info.metadactyl.base-url")

(cc/defprop-str metadactyl-unprotected-base-url
  "The base URL to use when connecting to unsecured Metadactyl services."
  [props config-valid configs app-routes-enabled]
  "data-info.metadactyl.unprotected-base-url")

(cc/defprop-str notificationagent-base-url
  "The base URL to use when connecting to the notification agent."
  [props config-valid configs notification-routes-enabled]
  "data-info.notificationagent.base-url")

(cc/defprop-str userinfo-base-url
  "The base URL for the user info API."
  [props config-valid configs user-info-routes-enabled]
  "data-info.userinfo.base-url")

(cc/defprop-str userinfo-key
  "The key to use when authenticating to the user info API."
  [props config-valid configs user-info-routes-enabled]
  "data-info.userinfo.client-key")

(cc/defprop-str userinfo-secret
  "The secret to use when authenticating to the user info API."
  [props config-valid configs user-info-routes-enabled]
  "data-info.userinfo.password")

(cc/defprop-str jex-base-url
  "The base URL for the JEX."
  [props config-valid configs app-routes-enabled]
  "data-info.jex.base-url")

;;;RabbitMQ connection information
(cc/defprop-str rabbitmq-host
  "The hostname for RabbitMQ"
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.host")

(cc/defprop-int rabbitmq-port
  "The port for RabbitMQ"
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.port")

(cc/defprop-str rabbitmq-user
  "The username for RabbitMQ"
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.user")

(cc/defprop-str rabbitmq-pass
  "The password for RabbitMQ"
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.pass")

(cc/defprop-str rabbitmq-exchange
  "The exchange to listen to for iRODS updates."
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.exchange")

(cc/defprop-str rabbitmq-exchange-type
  "The exchange type for the iRODS updates"
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.exchange.type")

(cc/defprop-boolean rabbitmq-exchange-durable?
  "Toggles whether or not the rabbitmq exchange is durable."
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.exchange.durable")

(cc/defprop-boolean rabbitmq-exchange-auto-delete?
  "Toggles whether to auto-delete the exchange or not."
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.exchange.auto-delete")

(cc/defprop-boolean rabbitmq-msg-auto-ack?
  "Toggles whether or not to auto-ack messages that are received."
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.msg.auto-ack")

(cc/defprop-long rabbitmq-health-check-interval
  "The number of milliseconds to wait between connection health checks."
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.connection.health-check-interval")

(cc/defprop-str rabbitmq-routing-key
  "The routing key for messages."
  [props config-valid configs rabbitmq-enabled]
  "data-info.rabbitmq.queue.routing-key")
;;;End RabbitMQ connection information

;;;iRODS connection information
(cc/defprop-str irods-home
  "Returns the path to the home directory in iRODS. Usually /iplant/home"
  [props config-valid configs data-routes-enabled]
  "data-info.irods.home")

(cc/defprop-str irods-user
  "Returns the user that porklock should connect as."
  [props config-valid configs data-routes-enabled]
  "data-info.irods.user")

(cc/defprop-str irods-pass
  "Returns the iRODS user's password."
  [props config-valid configs data-routes-enabled]
  "data-info.irods.pass")

(cc/defprop-str irods-host
  "Returns the iRODS hostname/IP address."
  [props config-valid configs data-routes-enabled]
  "data-info.irods.host")

(cc/defprop-str irods-port
  "Returns the iRODS port."
  [props config-valid configs data-routes-enabled]
  "data-info.irods.port")

(cc/defprop-str irods-zone
  "Returns the iRODS zone."
  [props config-valid configs data-routes-enabled]
  "data-info.irods.zone")

(cc/defprop-optstr irods-resc
  "Returns the iRODS resource."
  [props config-valid configs data-routes-enabled]
  "data-info.irods.resc")

(cc/defprop-int irods-max-retries
  "The number of retries for failed operations."
  [props config-valid configs data-routes-enabled]
  "data-info.irods.max-retries")

(cc/defprop-int irods-retry-sleep
  "The number of milliseconds to sleep between retries."
  [props config-valid configs data-routes-enabled]
  "data-info.irods.retry-sleep")

(cc/defprop-boolean irods-use-trash
  "Toggles whether to move deleted files to the trash first."
  [props config-valid configs data-routes-enabled]
  "data-info.irods.use-trash")

(cc/defprop-vec irods-admins
  "The admin users in iRODS."
  [props config-valid configs fileio-routes-enabled]
  "data-info.irods.admin-users")
;;;End iRODS connection information

;;;Database connection information
(cc/defprop-str db-driver-class
  "The name of the JDBC driver to use."
  [props config-valid configs]
  "data-info.db.driver" )

(cc/defprop-str db-subprotocol
  "The subprotocol to use when connecting to the database (e.g.
   postgresql)."
  [props config-valid configs]
  "data-info.db.subprotocol")

(cc/defprop-str db-host
  "The host name or IP address to use when
   connecting to the database."
  [props config-valid configs]
  "data-info.db.host")

(cc/defprop-str db-port
  "The port number to use when connecting to the database."
  [props config-valid configs]
  "data-info.db.port")

(cc/defprop-str db-name
  "The name of the database to connect to."
  [props config-valid configs]
  "data-info.db.name")

(cc/defprop-str db-user
  "The username to use when authenticating to the database."
  [props config-valid configs]
  "data-info.db.user")

(cc/defprop-str db-password
  "The password to use when authenticating to the database."
  [props config-valid configs]
  "data-info.db.password")
;;;End database connection information

;;;Metadata database connection information
(cc/defprop-str metadata-db-driver-class
  "The name of the JDBC driver to use."
  [props config-valid configs]
  "data-info.metadata.driver" )

(cc/defprop-str metadata-db-subprotocol
  "The subprotocol to use when connecting to the database (e.g.
   postgresql)."
  [props config-valid configs]
  "data-info.metadata.subprotocol")

(cc/defprop-str metadata-db-host
  "The host name or IP address to use when
   connecting to the database."
  [props config-valid configs]
  "data-info.metadata.host")

(cc/defprop-str metadata-db-port
  "The port number to use when connecting to the database."
  [props config-valid configs]
  "data-info.metadata.port")

(cc/defprop-str metadata-db-name
  "The name of the database to connect to."
  [props config-valid configs]
  "data-info.metadata.db")

(cc/defprop-str metadata-db-user
  "The username to use when authenticating to the database."
  [props config-valid configs]
  "data-info.metadata.user")

(cc/defprop-str metadata-db-password
  "The password to use when authenticating to the database."
  [props config-valid configs]
  "data-info.metadata.password")
;;;End Metadata database connection information

;;;OSM connection information
(cc/defprop-str osm-base-url
  "The base URL to use when connecting to the OSM."
  [props config-valid configs]
  "data-info.osm.base-url")

(cc/defprop-str osm-jobs-bucket
  "The OSM bucket containing information about jobs that the user has
   submitted."
  [props config-valid configs]
  "data-info.osm.jobs-bucket")
;;;End OSM connection information


;;; ICAT connection information
(cc/defprop-str icat-host
  "The hostname for the server running the ICAT database."
  [props config-valid configs data-routes-enabled]
  "data-info.icat.host")

(cc/defprop-int icat-port
  "The port that the ICAT is accepting connections on."
  [props config-valid configs data-routes-enabled]
  "data-info.icat.port")

(cc/defprop-str icat-user
  "The user for the ICAT database."
  [props config-valid configs data-routes-enabled]
  "data-info.icat.user")

(cc/defprop-str icat-password
  "The password for the ICAT database."
  [props config-valid configs data-routes-enabled]
  "data-info.icat.password")

(cc/defprop-str icat-db
  "The database name for the ICAT database. Yeah, it's most likely going to be 'ICAT'."
  [props config-valid configs data-routes-enabled]
  "data-info.icat.db")
;;; End ICAT connection information.

;;; Garnish configuration
(cc/defprop-str garnish-type-attribute
  "The value that goes in the attribute column for AVUs that define a file type."
  [props config-valid configs data-routes-enabled]
  "data-info.garnish.type-attribute")

(cc/defprop-str filetype-script
  "The path to a perl script that detects filetypes."
  [props config-valid configs data-routes-enabled]
  "data-info.garnish.filetype-script")

(cc/defprop-long filetype-read-amount
  "The size, in bytes as a long, of the sample read from iRODS"
  [props config-valid configs data-routes-enabled]
  "data-info.garnish.filetype-read-amount")
;;; End of Garnish configuration

;;; File IO configuration
(cc/defprop-str fileio-temp-dir
  "The directory, in iRODS, to use as temp storage for uploads."
  [props config-valid configs fileio-routes-enabled]
  "data-info.fileio.temp-dir")

(cc/defprop-str fileio-curl-path
  "The path on the cluster to the curl tool."
  [props config-valid configs fileio-routes-enabled]
  "data-info.fileio.curl-path")

(cc/defprop-str fileio-service-name
  "The old service name for fileio"
  [props config-valid configs fileio-routes-enabled]
  "data-info.fileio.service-name")

(cc/defprop-int fileio-max-edit-file-size
  "The old service name for fileio"
  [props config-valid configs fileio-routes-enabled]
  "data-info.fileio.max-edit-file-size")
;;; End File IO configuration

;;; Filesystem configuration (a.k.a. data-info).
(cc/defprop-long fs-preview-size
  "The size, in bytes, of the generated previews."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.preview-size")

(cc/defprop-int fs-data-threshold
  "Um...hmmm..."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.data-threshold")

(cc/defprop-str fs-community-data
  "The path to the root directory for community data."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.community-data")

(cc/defprop-vec fs-filter-files
  "The files to filter out of return values."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.filter-files")

(cc/defprop-vec fs-perms-filter
  "Hmmm..."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.perms-filter")

(cc/defprop-str fs-copy-attribute
  "The attribute to tag files with when they're a copy of another file."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.copy-key")

(cc/defprop-str fs-filter-chars
  "The characters that are considered invalid in iRODS dir- and filenames."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.filter-chars")

(cc/defprop-int fs-max-paths-in-request
  "The number of paths that are allowable in an API request."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.max-paths-in-request")

(cc/defprop-str fs-anon-user
  "The name of the anonymous user."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.anon-user")

(cc/defprop-str anon-files-base-url
  "The base url for the anon-files server."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.anon-files-base-url")
;;; End Filesystem configuration

(cc/defprop-int default-user-search-result-limit
  "The default limit for the number of results for a user info search.  Note
   this is the maximum number of results returned by trellis for any given
   search.  Our aggregate search may return the limit times the number of
   search types."
  [props config-valid configs user-info-routes-enabled]
  "data-info.userinfo.default-search-limit")

(cc/defprop-optint default-search-result-limit
  "This is the default limit for the number of results for a data search."
  [props config-valid configs search-routes-enabled]
  "data-info.search.default-limit" 50)

(cc/defprop-str data-info-base-url
  "The base URL for the data-info data management services."
  [props config-valid configs app-routes-enabled data-routes-enabled
   tree-viewer-routes-enabled]
  "data-info.nibblonian.base-url")

(cc/defprop-str scruffian-base-url
  "The base URL for the Scruffian file export and import services."
  [props config-valid configs tree-viewer-routes-enabled]
  "data-info.scruffian.base-url")

(cc/defprop-str tree-parser-url
  "The URL for the tree parser service."
  [props config-valid configs tree-viewer-routes-enabled]
  "data-info.tree-viewer.base-url")

(cc/defprop-str es-url
  "The URL for Elastic Search"
  [props config-valid configs data-routes-enabled]
  "data-info.infosquito.es-url")

(cc/defprop-str agave-base-url
  "The base URL to use when connecting to Agave."
  [props config-valid configs agave-enabled]
  "data-info.agave.base-url")

(cc/defprop-str agave-key
  "The API key to use when authenticating to Agave."
  [props config-valid configs agave-enabled]
  "data-info.agave.key")

(cc/defprop-str agave-secret
  "The API secret to use when authenticating to Agave."
  [props config-valid configs agave-enabled]
  "data-info.agave.secret")

(cc/defprop-str agave-oauth-base
  "The base URL for the Agave OAuth 2.0 endpoints."
  [props config-valid configs agave-enabled]
  "data-info.agave.oauth-base")

(cc/defprop-int agave-oauth-refresh-window
  "The number of minutes before a token expires to refresh it."
  [props config-valid configs agave-enabled]
  "data-info.agave.oauth-refresh-window")

(cc/defprop-str agave-redirect-uri
  "The redirect URI used after Agave authorization."
  [props config-valid configs agave-enabled]
  "data-info.agave.redirect-uri")

(cc/defprop-str agave-callback-base
  "The base URL for receiving job status update callbacks from Agave."
  [props config-valid configs #(and (agave-enabled) (agave-jobs-enabled))]
  "data-info.agave.callback-base")

(cc/defprop-optstr agave-storage-system
  "The storage system that Agave should use when interacting with the DE."
  [props config-valid configs agave-enabled]
  "data-info.agave.storage-system"
  "data.iplantcollaborative.org")

(cc/defprop-str coge-genome-load-url
  "The COGE service URL for loading genomes and creating viewer URLs."
  [props config-valid configs coge-enabled]
  "data-info.coge.genome-load-url")

(cc/defprop-str coge-user
  "The COGE user that needs file sharing permissions for genome viewer services."
  [props config-valid configs coge-enabled]
  "data-info.coge.user")

(cc/defprop-str kifshare-download-template
  "The mustache template for the kifshare URL."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.kifshare-download-template")

(cc/defprop-str kifshare-external-url
  "The external URL for kifshare."
  [props config-valid configs filesystem-routes-enabled]
  "data-info.fs.kifshare-external-url")

(cc/defprop-str default-output-dir
  "The default name of the default job output directory."
  [props config-valid configs]
  "data-info.job-exec.default-output-folder")

(cc/defprop-str prefs-base-url
  "The hostname of the user-preferences service"
  [props config-valid configs]
  "data-info.preferences.host")

(cc/defprop-str sessions-base-url
  "The hostname of the user-sessions service"
  [props config-valid configs]
  "data-info.sessions.host")

(cc/defprop-str saved-searches-base-url
  "The base URL of the saved-searches service"
  [props config-valid configs]
  "data-info.saved-searches.host")

(cc/defprop-str tree-urls-base-url
  "The base URL of the tree-urls service"
  [props config-valid configs]
  "data-info.tree-urls.host")

(cc/defprop-optstr keyring-path
  "The path to the secure PGP keyring."
  [props config-valid configs]
  "data-info.pgp.keyring-path"
  "/etc/iplant/de/crypto/secring.gpg")

(cc/defprop-str key-password
  "The password needed to unlock the PGP password."
  [props config-valid configs]
  "data-info.pgp.key-password")

(cc/defprop-str data-info-base-url
  "The data-info base URL, which is used to build callback URLs for other DE components."
  [props config-valid configs]
  "data-info.base-url")

(cc/defprop-int data-info-job-status-poll-interval
  "The job status polling interval in minutes."
  [props config-valid configs]
  "data-info.jobs.poll-interval")

(cc/defprop-str workspace-root-app-category
  "The name of the root app category in a user's workspace."
  [props config-valid configs]
  "data-info.workspace.root-app-category")

(cc/defprop-str workspace-default-app-categories
  "The names of the app categories immediately under the root app category in a user's workspace."
  [props config-valid configs]
  "data-info.workspace.default-app-categories")

(def get-default-app-categories
  (memoize
    (fn []
      (cheshire/decode (workspace-default-app-categories) true))))

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn- exception-filters
  []
  (filter #(not (nil? %))
          [(icat-password) (icat-user) (irods-pass) (irods-user) (agave-key) (agave-secret)]))

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

(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props :filters [#"irods\.user" #"icat\.user" #"oauth\.pem"])
  (validate-config)
  (ce/register-filters (exception-filters)))

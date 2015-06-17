(ns donkey.util.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clojure.string :as string]
            [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]
            [common-cfg.cfg :as cfg]
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

(defn masked-config
  "Returns a masked version of the Donkey config as a map."
  []
  (cc/mask-config props :filters [#"(?:irods)[-.](?:user|pass|key|secret)"]))

(cc/defprop-int listen-port
  "The port that donkey listens to."
  [props config-valid configs]
  "donkey.app.listen-port")

(cc/defprop-str environment-name
  "The name of the environment that this instance of Donkey belongs to."
  [props config-valid configs]
  "donkey.app.environment-name")

(cc/defprop-str cas-server
  "The base URL used to connect to the CAS server."
  [props config-valid configs]
  "donkey.cas.cas-server")

(cc/defprop-str pgt-callback-base
  "The base URL to use for proxy granting ticket callbacks from the CAS server."
  [props config-valid configs]
  "donkey.cas.pgt-callback-base")

(cc/defprop-str pgt-callback-path
  "The URL path to use for proxy granting ticket callbacks from the CAS server."
  [props config-valid configs]
  "donkey.cas.pgt-callback-path")

(cc/defprop-str server-name
  "The name of the local server."
  [props config-valid configs]
  "donkey.cas.server-name")

(cc/defprop-str group-attr-name
  "The name of the user attribute containing group membership information."
  [props config-valid configs]
  "donkey.cas.group-attr-name")

(cc/defprop-vec allowed-groups
  "The names of the groups that are permitted to access secured admin services."
  [props config-valid configs]
  "donkey.cas.allowed-groups")

(cc/defprop-str uid-domain
  "The domain name to append to the user identifier to get the fully qualified
   user identifier."
  [props config-valid configs]
  "donkey.uid.domain")

(cc/defprop-optboolean admin-routes-enabled
  "Enables or disables the administration routes."
  [props config-valid configs]
  "donkey.routes.admin" true)

(cc/defprop-optboolean notification-routes-enabled
  "Enables or disables notification endpoints."
  [props config-valid configs]
  "donkey.routes.notifications" true)

(cc/defprop-optboolean app-routes-enabled
  "Enables or disables app endpoints."
  [props config-valid configs]
  "donkey.routes.apps" true)

(cc/defprop-optboolean metadata-routes-enabled
  "Enables or disables metadata endpoints."
  [props config-valid configs]
  "donkey.routes.metadata" true)

(cc/defprop-optboolean pref-routes-enabled
  "Enables or disables preferences endpoints."
  [props config-valid configs]
  "donkey.routes.prefs" true)

(cc/defprop-optboolean user-info-routes-enabled
  "Enables or disables user-info endpoints."
  [props config-valid configs]
  "donkey.routes.user-info" true)

(cc/defprop-optboolean data-routes-enabled
  "Enables or disables data endpoints."
  [props config-valid configs]
  "donkey.routes.data" true)

(cc/defprop-optboolean tree-viewer-routes-enabled
  "Enables or disables tree-viewer endpoints."
  [props config-valid configs]
  "donkey.routes.tree-viewer" true)

(cc/defprop-optboolean session-routes-enabled
  "Enables or disables user session endpoints."
  [props config-valid configs]
  "donkey.routes.session" true)

(cc/defprop-optboolean collaborator-routes-enabled
  "Enables or disables collaborator routes."
  [props config-valid configs]
  "donkey.routes.collaborator" true)

(cc/defprop-optboolean fileio-routes-enabled
  "Enables or disables the fileio routes."
  [props config-valid configs]
  "donkey.routes.fileio" true)

(cc/defprop-optboolean filesystem-routes-enabled
  "Enables or disables the filesystem routes."
  [props config-valid configs]
  "donkey.routes.filesystem" true)

(cc/defprop-optboolean search-routes-enabled
  "Enables or disables the search related routes."
  [props config-valid configs]
  "donkey.routes.search" false)

(cc/defprop-optboolean coge-enabled
  "Enables or disables COGE endpoints."
  [props config-valid configs]
  "donkey.routes.coge" true)

(cc/defprop-str iplant-email-base-url
  "The base URL to use when connnecting to the iPlant email service."
  [props config-valid configs app-routes-enabled]
  "donkey.email.base-url")

(cc/defprop-str tool-request-dest-addr
  "The destination email address for tool request messages."
  [props config-valid configs app-routes-enabled]
  "donkey.email.tool-request-dest")

(cc/defprop-str tool-request-src-addr
  "The source email address for tool request messages."
  [props config-valid configs app-routes-enabled]
  "donkey.email.tool-request-src")

(cc/defprop-str feedback-dest-addr
  "The destination email address for DE feedback messages."
  [props config-valid configs app-routes-enabled]
  "donkey.email.feedback-dest")

(cc/defprop-str metadactyl-base-url
  "The base URL to use when connecting to secured Metadactyl services."
  [props config-valid configs app-routes-enabled]
  "donkey.metadactyl.base-url")

(def metadactyl-base
  (memoize
   (fn []
     (if (System/getenv "METADACTYL_PORT")
       (cfg/env-setting "METADACTYL_PORT")
       (metadactyl-base-url)))))

(cc/defprop-str metadata-base-url
  "The base URL to use when connecting to the metadata services."
  [props config-valid configs metadata-routes-enabled]
  "donkey.metadata.base-url")

(def metadata-base
  (memoize
   (fn []
     (if (System/getenv "METADATA_PORT")
       (cfg/env-setting "METADATA_PORT")
       (metadata-base-url)))))

(cc/defprop-str notificationagent-base-url
  "The base URL to use when connecting to the notification agent."
  [props config-valid configs notification-routes-enabled]
  "donkey.notificationagent.base-url")

(def notificationagent-base
  (memoize
   (fn []
     (if (System/getenv "NOTIFICATIONAGENT_PORT")
       (cfg/env-setting "NOTIFICATIONAGENT_PORT")
       (notificationagent-base-url)))))

(cc/defprop-str userinfo-base-url
  "The base URL for the user info API."
  [props config-valid configs user-info-routes-enabled]
  "donkey.userinfo.base-url")

(cc/defprop-str userinfo-key
  "The key to use when authenticating to the user info API."
  [props config-valid configs user-info-routes-enabled]
  "donkey.userinfo.client-key")

(cc/defprop-str userinfo-secret
  "The secret to use when authenticating to the user info API."
  [props config-valid configs user-info-routes-enabled]
  "donkey.userinfo.password")

(cc/defprop-str jex-base-url
  "The base URL for the JEX."
  [props config-valid configs app-routes-enabled]
  "donkey.jex.base-url")


;;;iRODS connection information
(cc/defprop-str irods-home
  "Returns the path to the home directory in iRODS. Usually /iplant/home"
  [props config-valid configs data-routes-enabled]
  "donkey.irods.home")

(cc/defprop-str irods-user
  "Returns the user that porklock should connect as."
  [props config-valid configs data-routes-enabled]
  "donkey.irods.user")

(cc/defprop-str irods-pass
  "Returns the iRODS user's password."
  [props config-valid configs data-routes-enabled]
  "donkey.irods.pass")

(cc/defprop-str irods-host
  "Returns the iRODS hostname/IP address."
  [props config-valid configs data-routes-enabled]
  "donkey.irods.host")

(cc/defprop-str irods-port
  "Returns the iRODS port."
  [props config-valid configs data-routes-enabled]
  "donkey.irods.port")

(cc/defprop-str irods-zone
  "Returns the iRODS zone."
  [props config-valid configs data-routes-enabled]
  "donkey.irods.zone")

(cc/defprop-optstr irods-resc
  "Returns the iRODS resource."
  [props config-valid configs data-routes-enabled]
  "donkey.irods.resc")

(cc/defprop-int irods-max-retries
  "The number of retries for failed operations."
  [props config-valid configs data-routes-enabled]
  "donkey.irods.max-retries")

(cc/defprop-int irods-retry-sleep
  "The number of milliseconds to sleep between retries."
  [props config-valid configs data-routes-enabled]
  "donkey.irods.retry-sleep")

(cc/defprop-boolean irods-use-trash
  "Toggles whether to move deleted files to the trash first."
  [props config-valid configs data-routes-enabled]
  "donkey.irods.use-trash")

(cc/defprop-vec irods-admins
  "The admin users in iRODS."
  [props config-valid configs fileio-routes-enabled]
  "donkey.irods.admin-users")
;;;End iRODS connection information

;;; ICAT connection information
(cc/defprop-str icat-host
  "The hostname for the server running the ICAT database."
  [props config-valid configs data-routes-enabled]
  "donkey.icat.host")

(cc/defprop-int icat-port
  "The port that the ICAT is accepting connections on."
  [props config-valid configs data-routes-enabled]
  "donkey.icat.port")

(cc/defprop-str icat-user
  "The user for the ICAT database."
  [props config-valid configs data-routes-enabled]
  "donkey.icat.user")

(cc/defprop-str icat-password
  "The password for the ICAT database."
  [props config-valid configs data-routes-enabled]
  "donkey.icat.password")

(cc/defprop-str icat-db
  "The database name for the ICAT database. Yeah, it's most likely going to be 'ICAT'."
  [props config-valid configs data-routes-enabled]
  "donkey.icat.db")
;;; End ICAT connection information.

;;; Garnish configuration
(cc/defprop-str garnish-type-attribute
  "The value that goes in the attribute column for AVUs that define a file type."
  [props config-valid configs data-routes-enabled]
  "donkey.garnish.type-attribute")


(cc/defprop-long filetype-read-amount
  "The size, in bytes as a long, of the sample read from iRODS"
  [props config-valid configs data-routes-enabled]
  "donkey.garnish.filetype-read-amount")
;;; End of Garnish configuration

;;; File IO configuration
(cc/defprop-str fileio-temp-dir
  "The directory, in iRODS, to use as temp storage for uploads."
  [props config-valid configs fileio-routes-enabled]
  "donkey.fileio.temp-dir")

(cc/defprop-uuid fileio-url-import-app
  "The identifier of the internal app used for URL imports."
  [props config-valid configs fileio-routes-enabled]
  "donkey.fileio.url-import-app")

(cc/defprop-int fileio-max-edit-file-size
  "The old service name for fileio"
  [props config-valid configs fileio-routes-enabled]
  "donkey.fileio.max-edit-file-size")
;;; End File IO configuration

;;; Filesystem configuration (a.k.a. data-info).

(cc/defprop-long fs-preview-size
  "The size, in bytes, of the generated previews."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.preview-size")

(cc/defprop-int fs-data-threshold
  "Um...hmmm..."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.data-threshold")

(cc/defprop-str fs-community-data
  "The path to the root directory for community data."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.community-data")

(cc/defprop-vec fs-bad-names
  "The bad data names."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.bad-names")

(cc/defprop-vec fs-perms-filter
  "Hmmm..."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.perms-filter")

(cc/defprop-str fs-copy-attribute
  "The attribute to tag files with when they're a copy of another file."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.copy-key")

(cc/defprop-str fs-bad-chars
  "The characters that are considered invalid in iRODS dir- and filenames."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.bad-chars")

(cc/defprop-int fs-max-paths-in-request
  "The number of paths that are allowable in an API request."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.max-paths-in-request")

(cc/defprop-str fs-anon-user
  "The name of the anonymous user."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.anon-user")

(cc/defprop-str anon-files-base-url
  "The base url for the anon-files server."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.anon-files-base-url")

(def anon-files-base
  (memoize
   (fn []
     (if (System/getenv "ANON_FILES_PORT")
       (cfg/env-setting "ANON_FILES_PORT")
       (anon-files-base-url)))))
;;; End Filesystem configuration

(cc/defprop-int default-user-search-result-limit
  "The default limit for the number of results for a user info search.  Note
   this is the maximum number of results returned by trellis for any given
   search.  Our aggregate search may return the limit times the number of
   search types."
  [props config-valid configs user-info-routes-enabled]
  "donkey.userinfo.default-search-limit")

(cc/defprop-optint default-search-result-limit
  "This is the default limit for the number of results for a data search."
  [props config-valid configs search-routes-enabled]
  "donkey.search.default-limit" 50)

(cc/defprop-str data-info-base-url
  "The base URL for the data info service."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.data-info.base-url")

(def data-info-base
  (memoize
   (fn []
     (if (System/getenv "DATA_INFO_PORT")
       (cfg/env-setting "DATA_INFO_PORT")
       (data-info-base-url)))))

(cc/defprop-str tree-parser-url
  "The URL for the tree parser service."
  [props config-valid configs tree-viewer-routes-enabled]
  "donkey.tree-viewer.base-url")

(cc/defprop-str es-url
  "The URL for Elastic Search"
  [props config-valid configs data-routes-enabled]
  "donkey.infosquito.es-url")

(cc/defprop-str coge-genome-load-url
  "The COGE service URL for loading genomes and creating viewer URLs."
  [props config-valid configs coge-enabled]
  "donkey.coge.genome-load-url")

(cc/defprop-str coge-user
  "The COGE user that needs file sharing permissions for genome viewer services."
  [props config-valid configs coge-enabled]
  "donkey.coge.user")

(cc/defprop-str kifshare-download-template
  "The mustache template for the kifshare URL."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.kifshare-download-template")

(cc/defprop-str kifshare-external-url
  "The external URL for kifshare."
  [props config-valid configs filesystem-routes-enabled]
  "donkey.fs.kifshare-external-url")

(cc/defprop-str default-output-dir
  "The default name of the default job output directory."
  [props config-valid configs]
  "donkey.job-exec.default-output-folder")

(cc/defprop-str prefs-base-url
  "The hostname of the user-preferences service"
  [props config-valid configs]
  "donkey.preferences.host")

(def prefs-base
  (memoize
   (fn []
     (if (System/getenv "USER_PREFERENCES_PORT")
       (cfg/env-setting "USER_PREFERENCES_PORT")
       (prefs-base-url)))))

(cc/defprop-str sessions-base-url
  "The hostname of the user-sessions service"
  [props config-valid configs]
  "donkey.sessions.host")

(def sessions-base
  (memoize
   (fn []
     (if (System/getenv "USER_SESSIONS_PORT")
       (cfg/env-setting "USER_SESSIONS_PORT")
       (sessions-base-url)))))

(cc/defprop-str saved-searches-base-url
  "The base URL of the saved-searches service"
  [props config-valid configs]
  "donkey.saved-searches.host")

(def saved-searches-base
  (memoize
   (fn []
     (if (System/getenv "SAVED_SEARCHES_PORT")
       (cfg/env-setting "SAVED_SEARCHES_PORT")
       (saved-searches-base-url)))))

(cc/defprop-str tree-urls-base-url
  "The base URL of the tree-urls service"
  [props config-valid configs]
  "donkey.tree-urls.host")

(def tree-urls-base
  (memoize
   (fn []
     (if (System/getenv "TREE_URLS_PORT")
       (cfg/env-setting "TREE_URLS_PORT")
       (tree-urls-base-url)))))

(cc/defprop-str donkey-base-url
  "The Donkey base URL, which is used to build callback URLs for other DE components."
  [props config-valid configs]
  "donkey.base-url")

(def get-allowed-groups
  (memoize
    (fn []
      (map #(string/replace (string/trim %) #"^'(.*)'$" "$1") (allowed-groups)))))

(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))

(defn- exception-filters
  []
  (filter #(not (nil? %))
          [(icat-password) (icat-user) (irods-pass) (irods-user)]))

(defn- oauth-settings
  [api-name api-key api-secret token-uri redirect-uri refresh-window]
  {:api-name       api-name
   :client-key     api-key
   :client-secret  api-secret
   :token-uri      token-uri
   :redirect-uri   redirect-uri
   :refresh-window (* refresh-window 60 1000)})

(defn log-environment
  []
  (log/warn "ENV? donkey.data-info.base-url -" (data-info-base))
  (log/warn "ENV? donkey.metadactyl.base-url =" (metadactyl-base))
  (log/warn "ENV? donkey.notificationagent.base-url =" (notificationagent-base))
  (log/warn "ENV? donkey.anon-files.base-url =" (anon-files-base))
  (log/warn "ENV? donkey.sessions.host =" (sessions-base))
  (log/warn "ENV? donkey.saved-searches.host =" (saved-searches-base))
  (log/warn "ENV? donkey.tree-urls.host =" (tree-urls-base))
  (log/warn "ENV? donkey.preferences.host =" (prefs-base)))

(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props :filters [#"irods\.user" #"icat\.user" #"oauth\.pem"])
  (log-environment)
  (validate-config)
  (ce/register-filters (exception-filters)))

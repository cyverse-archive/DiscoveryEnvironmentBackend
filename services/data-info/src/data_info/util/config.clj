(ns data-info.util.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.core.memoize :as memo]
            [clj-jargon.init :as init]
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
  (cc/mask-config props :filters [#"(?:irods)[-.](?:user|pass)"]))


(cc/defprop-int listen-port
  "The port that data-info listens to."
  [props config-valid configs]
  "data-info.port")


(cc/defprop-long preview-size
  "The size, in bytes, of the generated previews."
  [props config-valid configs]
  "data-info.preview-size")


(cc/defprop-int data-threshold
  "Um...hmmm..."
  [props config-valid configs]
  "data-info.data-threshold")


(cc/defprop-str community-data
  "The path to the root directory for community data."
  [props config-valid configs]
  "data-info.community-data")


(cc/defprop-vec filter-files
  "The files to filter out of return values."
  [props config-valid configs]
  "data-info.filter-files")


(cc/defprop-vec perms-filter
  "Hmmm..."
  [props config-valid configs]
  "data-info.perms-filter")


(cc/defprop-str copy-attribute
  "The attribute to tag files with when they're a copy of another file."
  [props config-valid configs]
  "data-info.copy-key")


(cc/defprop-int max-paths-in-request
  "The number of paths that are allowable in an API request."
  [props config-valid configs]
  "data-info.max-paths-in-request")


(cc/defprop-str anon-user
  "The name of the anonymous user."
  [props config-valid configs]
  "data-info.anon-user")


(cc/defprop-str anon-files-base-url
  "The base url for the anon-files server."
  [props config-valid configs]
  "data-info.anon-files-base-url")


; AMQP connection information

(cc/defprop-str amqp-host
  "The hostname for AMQP"
  [props config-valid configs]
  "data-info.amqp.host")


(cc/defprop-int amqp-port
  "The port for AMQP"
  [props config-valid configs]
  "data-info.amqp.port")


(cc/defprop-str amqp-user
  "The username for AMQP"
  [props config-valid configs]
  "data-info.amqp.user")


(cc/defprop-str amqp-password
  "The password for AMQP"
  [props config-valid configs]
  "data-info.amqp.password")


(cc/defprop-str amqp-exchange-name
  "The exchange to listen to for iRODS updates."
  [props config-valid configs]
  "data-info.amqp.exchange.name")


(cc/defprop-str amqp-exchange-type
  "The exchange type for the iRODS updates"
  [props config-valid configs]
  "data-info.amqp.exchange.type")


(cc/defprop-boolean amqp-exchange-durable?
  "Toggles whether or not the amqp exchange is durable."
  [props config-valid configs]
  "data-info.amqp.exchange.durable")


(cc/defprop-boolean amqp-exchange-auto-delete?
  "Toggles whether to auto-delete the exchange or not."
  [props config-valid configs]
  "data-info.amqp.exchange.auto-delete")


(cc/defprop-boolean amqp-msg-auto-ack?
  "Toggles whether or not to auto-ack messages that are received."
  [props config-valid configs]
  "data-info.amqp.auto-ack")


(cc/defprop-str amqp-routing-key
  "The routing key for messages."
  [props config-valid configs]
  "data-info.amqp.routing-key")

; End AMQP connection information


; iRODS connection information

(cc/defprop-str irods-home
  "Returns the path to the home directory in iRODS. Usually /iplant/home"
  [props config-valid configs]
  "data-info.irods.home")


(cc/defprop-str irods-user
  "Returns the user that data-info should connect as."
  [props config-valid configs]
  "data-info.irods.user")


(cc/defprop-str irods-password
  "Returns the iRODS user's password."
  [props config-valid configs]
  "data-info.irods.password")


(cc/defprop-str irods-host
  "Returns the iRODS hostname/IP address."
  [props config-valid configs]
  "data-info.irods.host")


(cc/defprop-str irods-port
  "Returns the iRODS port."
  [props config-valid configs]
  "data-info.irods.port")


(cc/defprop-str irods-zone
  "Returns the iRODS zone."
  [props config-valid configs]
  "data-info.irods.zone")


(cc/defprop-optstr irods-resc
  "Returns the iRODS resource."
  [props config-valid configs]
  "data-info.irods.resc")


(cc/defprop-int irods-max-retries
  "The number of retries for failed operations."
  [props config-valid configs]
  "data-info.irods.max-retries")


(cc/defprop-int irods-retry-sleep
  "The number of milliseconds to sleep between retries."
  [props config-valid configs]
  "data-info.irods.retry-sleep")


(cc/defprop-boolean irods-use-trash
  "Toggles whether to move deleted files to the trash first."
  [props config-valid configs]
  "data-info.irods.use-trash")


(cc/defprop-vec irods-admins
  "The admin users in iRODS."
  [props config-valid configs]
  "data-info.irods.admin-users")

; End iRODS connection information


; ICAT connection information

(cc/defprop-str icat-host
  "The hostname for the server running the ICAT database."
  [props config-valid configs]
  "data-info.icat.host")


(cc/defprop-int icat-port
  "The port that the ICAT is accepting connections on."
  [props config-valid configs]
  "data-info.icat.port")


(cc/defprop-str icat-user
  "The user for the ICAT database."
  [props config-valid configs]
  "data-info.icat.user")


(cc/defprop-str icat-password
  "The password for the ICAT database."
  [props config-valid configs]
  "data-info.icat.password")


(cc/defprop-str icat-db
  "The database name for the ICAT database. Yeah, it's most likely going to be 'ICAT'."
  [props config-valid configs]
  "data-info.icat.db")

; End ICAT connection information.


; type detection configuration

(cc/defprop-str type-detect-type-attribute
  "The value that goes in the attribute column for AVUs that define a file type."
  [props config-valid configs]
  "data-info.type-detect.type-attribute")


(cc/defprop-str filetype-script
  "The path to a perl script that detects filetypes."
  [props config-valid configs]
  "data-info.type-detect.filetype-script")


(cc/defprop-long filetype-read-amount
  "The size, in bytes as a long, of the sample read from iRODS"
  [props config-valid configs]
  "data-info.type-detect.filetype-read-amount")

; End of type detection configuration


(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))


(defn- exception-filters
  []
  (filter #(not (nil? %)) [(icat-password) (icat-user) (irods-password) (irods-user)]))


(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props :filters [#"irods\.user" #"icat\.user"])
  (validate-config)
  (ce/register-filters (exception-filters)))


(def jargon-cfg
  (memo/memo #(init/init (irods-host)
                         (irods-port)
                         (irods-user)
                         (irods-password)
                         (irods-home)
                         (irods-zone)
                         (irods-resc)
                :max-retries (irods-max-retries)
                :retry-sleep (irods-retry-sleep)
                :use-trash   (irods-use-trash))))

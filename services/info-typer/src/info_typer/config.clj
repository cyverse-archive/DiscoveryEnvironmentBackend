(ns info-typer.config
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.config :as cc]
            [clojure-commons.error-codes :as ce]))


(def ^:private config-valid
  "A ref for storing a configuration validity flag."
  (ref true))


(def ^:private configs
  "A ref for storing the symbols used to get configuration settings."
  (ref []))


(def ^:private props
  "A ref for storing the configuration properties."
  (ref nil))


(cc/defprop-str garnish-type-attribute
  "The value that goes in the attribute column for AVUs that define a file type."
  [props config-valid configs]
  "info-typer.type-attribute")


(cc/defprop-long filetype-read-amount
  "The size, in bytes as a long, of the sample read from iRODS"
  [props config-valid configs]
  "info-typer.filetype-read-amount")


(cc/defprop-str irods-host
  "Returns the iRODS hostname/IP address."
  [props config-valid configs]
  "info-typer.irods.host")


(cc/defprop-str irods-port
  "Returns the iRODS port."
  [props config-valid configs]
  "info-typer.irods.port")


(cc/defprop-str irods-user
  "Returns the user that porklock should connect as."
  [props config-valid configs]
  "info-typer.irods.user")


(cc/defprop-str irods-pass
  "Returns the iRODS user's password."
  [props config-valid configs]
  "info-typer.irods.pass")


(cc/defprop-str irods-zone
  "Returns the iRODS zone."
  [props config-valid configs]
  "info-typer.irods.zone")


(cc/defprop-str irods-home
  "Returns the path to the home directory in iRODS. Usually /iplant/home"
  [props config-valid configs]
  "info-typer.irods.home")


(cc/defprop-optstr irods-resc
  "Returns the iRODS resource."
  [props config-valid configs]
  "info-typer.irods.resc")


(cc/defprop-int irods-max-retries
  "The number of retries for failed operations."
  [props config-valid configs]
  "info-typer.irods.max-retries")


(cc/defprop-int irods-retry-sleep
  "The number of milliseconds to sleep between retries."
  [props config-valid configs]
  "info-typer.irods.retry-sleep")


(cc/defprop-boolean irods-use-trash
  "Toggles whether to move deleted files to the trash first."
  [props config-valid configs]
  "info-typer.irods.use-trash")


(cc/defprop-str rabbitmq-host
  "The hostname for RabbitMQ"
  [props config-valid configs]
  "info-typer.rabbitmq.host")


(cc/defprop-int rabbitmq-port
  "The port for RabbitMQ"
  [props config-valid configs]
  "info-typer.rabbitmq.port")


(cc/defprop-str rabbitmq-user
  "The username for RabbitMQ"
  [props config-valid configs]
  "info-typer.rabbitmq.user")


(cc/defprop-str rabbitmq-pass
  "The password for RabbitMQ"
  [props config-valid configs]
  "info-typer.rabbitmq.pass")


(cc/defprop-long rabbitmq-health-check-interval
  "The number of milliseconds to wait between connection health checks."
  [props config-valid configs]
  "info-typer.rabbitmq.connection.health-check-interval")


(cc/defprop-str rabbitmq-exchange
  "The exchange to listen to for iRODS updates."
  [props config-valid configs]
  "info-typer.rabbitmq.exchange")


(cc/defprop-str rabbitmq-exchange-type
  "The exchange type for the iRODS updates"
  [props config-valid configs]
  "info-typer.rabbitmq.exchange.type")


(cc/defprop-boolean rabbitmq-exchange-durable?
  "Toggles whether or not the rabbitmq exchange is durable."
  [props config-valid configs]
  "info-typer.rabbitmq.exchange.durable")


(cc/defprop-boolean rabbitmq-exchange-auto-delete?
  "Toggles whether to auto-delete the exchange or not."
  [props config-valid configs]
  "info-typer.rabbitmq.exchange.auto-delete")


(cc/defprop-boolean rabbitmq-msg-auto-ack?
  "Toggles whether or not to auto-ack messages that are received."
  [props config-valid configs]
  "info-typer.rabbitmq.msg.auto-ack")


(cc/defprop-str rabbitmq-routing-key
  "The routing key for messages."
  [props config-valid configs]
  "info-typer.rabbitmq.queue.routing-key")


(defn- exception-filters
  []
  (filter #(not (nil? %)) [(irods-pass) (irods-user)]))


(defn- validate-config
  "Validates the configuration settings after they've been loaded."
  []
  (when-not (cc/validate-config configs config-valid)
    (throw+ {:error_code ce/ERR_CONFIG_INVALID})))


(defn load-config-from-file
  "Loads the configuration settings from a file."
  [cfg-path]
  (cc/load-config-from-file cfg-path props)
  (cc/log-config props :filters [#"irods\.user" #"icat\.user"])
  (validate-config)
  (ce/register-filters (exception-filters)))

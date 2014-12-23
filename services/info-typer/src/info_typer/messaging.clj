(ns info-typer.messaging
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [slingshot.slingshot :refer [throw+ try+]]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :as info]
            [clj-jargon.metadata :as meta]
            [clojure-commons.error-codes :as ce]
            [info-typer.amqp :as amqp]
            [info-typer.config :as cfg]
            [info-typer.irods :as irods])
  (:import [clojure.lang IPersistentMap]))


(defn- filetype-message-handler
  [irods-cfg payload]
  (try+
    (with-jargon irods-cfg [cm]
      (let [parsed-map (json/parse-string payload true)
            path       (:path parsed-map)]
        (if (nil? path)
          (throw+ {:error_code "ERR_INVALID_JSON" :payload payload}))
        (if-not (info/exists? cm path)
          (log/warn "[filetype-message-handler]" path
            "does not exist, it probably got moved before the handler fired."))
        (if (meta/attribute? cm path (cfg/garnish-type-attribute))
          (log/warn "[filetype-message-handler]" path "already has an attribute called"
            (cfg/garnish-type-attribute)))
        (when (and (info/exists? cm path)
                   (not (meta/attribute? cm path (cfg/garnish-type-attribute))))
          (let [ctype (irods/content-type cm path)]
            (when-not (or (nil? ctype) (string/blank? ctype))
              (log/warn "[filetype-message-handler] adding type" ctype "to" path)
              (meta/add-metadata cm path (cfg/garnish-type-attribute) ctype "")
              (log/warn "[filetype-message-handler] done adding type" ctype "to" path))
            (when (or (nil? ctype) (string/blank? ctype))
              (log/warn "[filetype-message-handler] type was not detected for" path))))))
    (catch ce/error? err
      (log/error (ce/format-exception (:throwable &throw-context))))
    (catch Exception e
      (log/error (ce/format-exception e)))))


(defn- dataobject-added
  "Event handler for 'data-object.added' events."
  [irods-cfg ^bytes payload]
  (filetype-message-handler irods-cfg (String. payload "UTF-8")))


(defn- message-handler
  "A langohr compatible message callback. This will push out message handling to other functions
   based on the value of the routing-key. This will allow us to pull in the full iRODS event
   firehose later and delegate execution to handlers without having to deal with AMQPs object
   hierarchy.

   The payload is passed to handlers as a byte stream. That should theoretically give us the
   ability to handle binary data arriving in messages, even though that doesn't seem likely."
  [irods-cfg channel {:keys [routing-key content-type delivery-tag type] :as meta} ^bytes payload]
  (log/info (format "[amqp/message-handler] [%s] [%s]" routing-key (String. payload "UTF-8")))
  (case routing-key
    "data-object.add" (dataobject-added irods-cfg payload)
    nil))


(defn receive
  "Configures the AMQP connection. This is wrapped in a function because we want to start
   the connection in a new thread."
  [^IPersistentMap irods-cfg]
  (try
    (amqp/configure (partial message-handler irods-cfg))
    (catch Exception e
      (log/error "[amqp/messaging-initialization]" (ce/format-exception e)))))

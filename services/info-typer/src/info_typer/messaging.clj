(ns info-typer.messaging
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [info-typer.amqp :as amqp]
            [info-typer.messages :as ftype])
  (:import [clojure.lang IPersistentMap]))


(defn- dataobject-added
  "Event handler for 'data-object.added' events."
  [irods-cfg ^bytes payload]
  (ftype/filetype-message-handler irods-cfg (String. payload "UTF-8")))


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

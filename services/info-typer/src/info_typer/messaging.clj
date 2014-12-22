(ns info-typer.messaging
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [info-typer.amqp :as amqp]
            [info-typer.messages :as ftype]))


(defn- dataobject-added
  "Event handler for 'data-object.added' events."
  [^bytes payload]
  (ftype/filetype-message-handler (String. payload "UTF-8")))


(defn- message-handler
  "A langohr compatible message callback. This will push out message handling to other functions
   based on the value of the routing-key. This will allow us to pull in the full iRODS event
   firehose later and delegate execution to handlers without having to deal with AMQPs object
   hierarchy.

   The payload is passed to handlers as a byte stream. That should theoretically give us the
   ability to handle binary data arriving in messages, even though that doesn't seem likely."
  [channel {:keys [routing-key content-type delivery-tag type] :as meta} ^bytes payload]
  (log/info (format "[amqp/message-handler] [%s] [%s]" routing-key (String. payload "UTF-8")))
  (case routing-key
    "data-object.add" (dataobject-added payload)
    nil))


(defn- receive
  "Configures the AMQP connection. This is wrapped in a function because we want to start
   the connection in a new thread."
  []
  (try
    (amqp/configure message-handler)
    (catch Exception e
      (log/error "[amqp/messaging-initialization]" (ce/format-exception e)))))


(defn- monitor
  "Fires off the monitoring thread that makes sure that the AMQP connection is still up
   and working."
  []
  (try
    (amqp/conn-monitor message-handler)
    (catch Exception e
      (log/error "[amqp/messaging-initialization]" (ce/format-exception e)))))


(defn messaging-initialization
  "Initializes the AMQP messaging handling, registering (message-handler) as the callback."
  []
  (.start (Thread. receive))
  (monitor))

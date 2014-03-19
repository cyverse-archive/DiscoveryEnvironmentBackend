(ns clojure-commons.infosquito
  "This namespace provides some types that can serve as wrappers for clients of various work
   queues. These types will all implement a single protocol and will thus serve as an abstraction
   layer that allows us to easily switch to different work queues if necessary.

   Creating a new client should automatically establish a connection to the server. In general,
   this means that a factory function should be used to generate each type of queue client. The
   factory function will establish the connection, pass it to the constructor of the appropriate
   QueueClient implementation, and return the newly created QueueClient implementation.

   Each QueueClient implementation must also implement java.io.Closeable. This allows the
   implementation to be used within a with-open macro to ensure that the connection to the server
   is closed cleanly."
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.consumers :as lc]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [langohr.basic :as lb]))

(defprotocol QueueClient

  (publish [this payload]
    "Publishes a message to the queue.")

  (subscribe [this handler]
    "Subscribes to the queue. The handler will be called every time a message is retrieved
     from the queue. This method blocks the calling thread, so it should normally be called
     from within a dedicated thread."))
;; QueueClient

(deftype RabbitmqClient [conn channel exchange queue]

  Closeable
  (close [this]
    (rmq/close channel)
    (rmq/close conn))

  QueueClient
  (publish [this payload]
    (lb/publish channel exchange queue payload))

  (subscribe [this handler]
    (lc/subscribe channel queue
                  (fn [ch {:keys [headers delivery-tag]} ^bytes payload]
                    (handler queue (String. payload "UTF-8"))
                    (lb/ack ch delivery-tag))
                  :auto-ack false)))
;; RabbitmqClient

(defn rabbitmq-connection-settings
  "Returns a connection settings map that can be used to establish a connection to RabbitMQ."
  [host port user pass]
  {:host     host
   :port     port
   :username user
   :password pass})

(defn rabbitmq-client
  "Creates a new RabbitmqClient instance."
  [connection-settings exchange queue]
  (let [conn    (rmq/connect connection-settings)
        channel (lch/open conn)]
    (le/declare channel exchange "direct" :durable true)
    (lq/declare channel queue :durable true :auto-delete false)
    (RabbitmqClient. conn channel exchange queue)))

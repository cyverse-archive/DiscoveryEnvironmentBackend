(ns data-info.clients.amqp
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.exchange :as le]
            [langohr.queue :as lq]
            [langohr.consumers :as lc]
            [langohr.basic :as lb]
            [data-info.util.config :as cfg]
            [clojure.tools.logging :as log]))


(defn test-msg-fn
  [channel {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (println (format "[data-info] Message: %s\tDelivery: %s\tContent Type: %s\tType: %s"
          (String. payload "UTF-8") delivery-tag content-type type)))

(defn connection
  "Creates and returns a new connection to the AMQP server."
  [conn-map]
  (rmq/connect conn-map))

(defn channel
  "Creates a channel on connection and returnes it."
  [connection]
  (lch/open connection))

(defn exchange?
  "Returns a boolean indicating whether an exchange exists."
  [channel exchange]
  (try
    (le/declare-passive channel exchange)
    true
    (catch java.io.IOException _ false)))

(defn declare-exchange
  "Declares an exchange if it doesn't already exist."
  [channel exchange type & {:keys [durable auto-delete]
                            :or {durable     false
                                 auto-delete false}}]
  (when-not (exchange? channel exchange)
    (le/declare channel exchange type :durable durable :auto-delete auto-delete))
  channel)

(defn declare-queue
  "Declares a default, anonymouse queue."
  [channel]
  (.getQueue (lq/declare channel)))

(defn bind
  "Binds a queue to an exchange."
  [channel queue exchange routing-key]
  (lq/bind channel queue exchange :routing-key routing-key)
  channel)

(defn publish
  "Publishes a message to an exchange."
  [channel exchange queue message]
  (lb/publish channel exchange queue message))

(defn subscribe
  "Registers a callback function that fires every time a message enters the specified queue."
  [channel queue msg-fn & {:keys [auto-ack]
                           :or   {auto-ack true}}]
  (lc/subscribe channel queue msg-fn :auto-ack true)
  channel)
(def amqp-conn (ref nil))
(def amqp-channel (ref nil))


(defn connection-map
  "Returns a configuration map for the AMQP connection."
  []
  {:host     (cfg/amqp-host)
   :port     (cfg/amqp-port)
   :username (cfg/amqp-user)
   :password (cfg/amqp-password)})


(defn connection-okay?
  "Returns a boolean telling whether the connection that's passed in is still active."
  [conn]
  (and (not (nil? conn))
       (rmq/open? conn)))

(defn channel-okay?
  "Returns a boolean telling whether the channel that's passed in is still active."
  [chan]
  (and (not (nil? chan))
       (rmq/open? chan)))

(defn get-connection
  "Sets the amqp-conn ref if necessary and returns it."
  []
  (if (connection-okay? @amqp-conn)
    @amqp-conn
    (dosync (ref-set amqp-conn (connection (connection-map))))))

(defn get-channel
  "Sets the amqp-channel ref if necessary and returns it."
  []
  (if (channel-okay? @amqp-channel)
    @amqp-channel
    (dosync (ref-set amqp-channel (channel (get-connection))))))


(defn configure
  "Sets up a channel, exchange, and queue, with the queue bound to the exchange and 'msg-fn' 
   registered as the callback."
  [msg-fn]
  (dosync
    (when (or (not (connection-okay? @amqp-conn)) 
              (not (channel-okay? @amqp-channel)))
      (log/info "[AMQP] configuring message connection")
      (let [conn (get-connection)
            chan (get-channel)
            q    (declare-queue @amqp-channel)]
        (declare-exchange
          @amqp-channel
          (cfg/amqp-exchange-name)
          (cfg/amqp-exchange-type)
          :durable     (cfg/amqp-exchange-durable?)
          :auto-delete (cfg/amqp-exchange-auto-delete?))
        (bind @amqp-channel q (cfg/amqp-exchange-name) (cfg/amqp-routing-key))
        (subscribe @amqp-channel q msg-fn :auto-ack (cfg/amqp-msg-auto-ack?))))))

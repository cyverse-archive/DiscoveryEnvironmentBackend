(ns info-typer.amqp
  (:require [clojure.tools.logging       :as log]
            [langohr.core                :as rmq]
            [langohr.channel             :as lch]
            [langohr.exchange            :as le]
            [langohr.queue               :as lq]
            [langohr.consumers           :as lc]
            [info-typer.config           :as cfg])
  (:import [java.net SocketException]))


(defn- connection-map
  "Returns a configuration map for the RabbitMQ connection."
  []
  {:host     (cfg/amqp-host)
   :port     (cfg/amqp-port)
   :username (cfg/amqp-user)
   :password (cfg/amqp-pass)})


(defn- attempt-connect
  [conn-map]
  (try
    (let [conn (rmq/connect conn-map)]
      (log/info "Connected to the AMQP broker.")
      conn)
    (catch SocketException e
      (log/warn "Failed to connect to the AMQP broker."))))


(defn- get-connection
  "Sets the amqp-conn ref if necessary and returns it."
  [conn-map]
  (if-let [conn (attempt-connect conn-map)]
    conn
    (do
      (Thread/sleep (cfg/amqp-retry-sleep))
      (recur conn-map))))


(defn- exchange?
  "Returns a boolean indicating whether an exchange exists."
  [channel exchange]
  (try
    (le/declare-passive channel exchange)
    true
    (catch java.io.IOException _ false)))


(defn- declare-exchange
  "Declares an exchange if it doesn't already exist."
  [channel exchange type & {:keys [durable auto-delete]
                            :or {durable     false
                                 auto-delete false}}]
  (when-not (exchange? channel exchange)
    (le/declare channel exchange type :durable durable :auto-delete auto-delete))
  channel)


(defn- declare-queue
  "Declares a default, anonymouse queue."
  [channel]
  (.getQueue (lq/declare channel)))


(defn- bind
  "Binds a queue to an exchange."
  [channel queue exchange routing-key]
  (lq/bind channel queue exchange {:routing-key routing-key})
  channel)


(defn- subscribe
  "Registers a callback function that fires every time a message enters the specified queue."
  [channel queue msg-fn & {:keys [auto-ack]
                           :or   {auto-ack true}}]
  (lc/subscribe channel queue msg-fn {:auto-ack true})
  channel)


(defn configure
  "Sets up a channel, exchange, and queue, with the queue bound to the exchange and 'msg-fn'
   registered as the callback."
  [msg-fn]
  (log/info "configuring AMQP connection")
  (let [chan (lch/open (get-connection (connection-map)))
        q    (declare-queue chan)]
    (declare-exchange chan (cfg/amqp-exchange) (cfg/amqp-exchange-type)
      :durable (cfg/amqp-exchange-durable?) :auto-delete (cfg/amqp-exchange-auto-delete?))
    (bind chan q (cfg/amqp-exchange) (cfg/amqp-routing-key))
    (subscribe chan q msg-fn :auto-ack (cfg/amqp-msg-auto-ack?))))

(ns monkey.messenger
  "This namespace implements the Messages protocol where langhor is used to interface with an AMQP
   broker."
  (:gen-class)
  (:require [clojure.tools.logging :as log]
            [lnagohr.basic :as basic]
            [langohr.channel :as ch]
            [langohr.consumers :as consumer]
            [langohr.core :as amqp]
            [langohr.exchange :as exchange]
            [langohr.queue :as queue]
            [monkey.props :as props])
  (:import [clojure.lang IFn PersistentArrayMap]))


(defn- attempt-connect
  [props]
  (try
    (let [conn (amqp/connect {:host     (props/amqp-host props)
                              :port     (props/amqp-port props)
                              :username (props/amqp-user props)
                              :password (props/amqp-password props)})]
      (log/info "successfully connected to AMQP broker")
      conn)
    (catch Throwable t
      (log/error t "failed to connect to AMQP broker"))))


(defn- connect
  [props]
  (if-let [conn (attempt-connect props)]
    conn
    (do
      (Thread/sleep (props/retry-period props))
      (recur props))))


(defn- prepare-queue
  [ch props]
  (let [exchange (props/amqp-exchange-name props)
        queue    (props/amqp-routing-key props)]
    (exchange/direct ch exchange
      :durable     (props/amqp-exchange-durable? props)
      :auto-delete (prop/amqp-exchange-auto-delete? props))
    (queue/declare ch queue :durable true)
    (queue/bind ch queue exchange :routing-key (props/amqp-routing-key props))
    queue))


(defn- log-registered
  [_]
  (log/info "registered with AMQP broker"))


(defn- handle-delivery
  [ch deliver {:keys [delivery-tag]} _]
  (try
    (deliver)
    (basic/ack ch delivery-tag)
    (catch Throwable t
      (log/error t "metadata reindexing failed, rescheduling")
      (basic/reject ch delivery-tag true))))


(defn- log-canceled
  [_]
  (log/info "AMQP broker registration canceled"))


(defn- receive
  [conn props notify-received]
  (let [ch       (ch/open conn)
        queue    (prepare-queue ch props)
        consumer (consumer/create-default ch
                   :handle-consume-ok-fn log-registered
                   :handle-delivery-fn   (partial handle-delivery ch notify-received)
                   :handle-cancel-fn     log-canceled)]
    (basic/consume ch queue consumer)))


(defn- silently-close
  [conn]
  (try
    (amqp/close conn)
    (catch Throwable _)))


(defn listen
  "This function monitors an AMQP exchange for tags reindexing messages. When it receives a message,
   it calls the provided function to trigger a reindexing. It never returns.

    Parameters:
      props           - the configuration properties
      notify-received - the function to call when a message is received"
  [^PersistentArrayMap props ^IFn notify-received]
  (let [conn (conect props)]
    (try
      (receive (connect props) props notify-received)
      (catch Throwable t
        (log/error t "reconnecting to AMQP broker"))
      (finally
        (silently-close conn))))
  (Thread/sleep (props/retry-period props))
  (recur props notify-received))

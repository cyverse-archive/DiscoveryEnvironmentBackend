(ns infosquito.messages
  (:require [clojure.tools.logging :as log]
            [infosquito.actions :as actions]
            [infosquito.props :as cfg]
            [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.queue :as lq]
            [langohr.consumers :as lc]
            [langohr.basic :as lb])
  (:import [java.io IOException]))

(def ^:const exchange "amq.direct")

(def ^:const initial-sleep-time 5000)
(def ^:const max-sleep-time 320000)

(defn- sleep
  [millis]
  (try
    (Thread/sleep millis)
    (catch InterruptedException _
      (log/warn "sleep interrupted"))))

(defn- connection-attempt
  [props millis-to-next-attempt]
  (try
    (rmq/connect {:host     (cfg/get-amqp-host props)
                  :port     (cfg/get-amqp-port props)
                  :username (cfg/get-amqp-user props)
                  :password (cfg/get-amqp-pass props)})
    (catch IOException e
      (log/error e "unable to establish AMQP connection - trying again in"
                 millis-to-next-attempt "milliseconds")
      (sleep millis-to-next-attempt))))

(defn- next-sleep-time
  [curr-sleep-time]
  (min max-sleep-time (* curr-sleep-time 2)))

(defn- amqp-connect
  "Repeatedly attempts to connect to the AMQP broker, sleeping for increasing periods of
   time when a connection can't be established."
  [props]
  (->> (iterate next-sleep-time initial-sleep-time)
       (map (partial connection-attempt props))
       (remove nil?)
       (first)))

(defn- declare-queue
  [ch queue-name]
  (lq/declare ch queue-name
              :durable     true
              :auto-delete false
              :exclusive   false)
  (lq/bind ch queue-name exchange :routing-key queue-name))

(defn- reindex-handler
  [props ch {:keys [delivery-tag]} _]
  (try
    (actions/reindex props)
    (lb/ack ch delivery-tag)
    (catch Throwable t
      (log/error t "data store reindexing failed")
      (log/warn "requeuing message after" (cfg/get-retry-interval props) "seconds")
      (Thread/sleep (cfg/get-retry-millis props))
      (lb/reject ch delivery-tag true))))

(defn- add-reindex-subscription
 [props ch]
 (let [queue-name (cfg/get-amqp-reindex-queue props)]
   (declare-queue ch queue-name)
   (lc/blocking-subscribe ch queue-name (partial reindex-handler props))))

(defn- rmq-close
  [c]
  (try
    (rmq/close c)
    (catch Exception _)))

(defn- subscribe
  [conn props]
  (let [ch (lch/open conn)]
    (try
      (add-reindex-subscription props ch)
      (catch Exception e (log/error e "error occurred during message processing"))
      (finally (rmq-close ch)))))

(defn repeatedly-connect
  "Repeatedly attempts to connect to the AMQP broker subscribe to incomming messages."
  [props]
  (let [conn (amqp-connect props)]
    (log/info "successfully connected to AMQP broker")
    (try
      (subscribe conn props)
      (catch Exception e (log/error e "reconnecting to AMQP in" initial-sleep-time "milliseconds"))
      (finally (rmq-close conn))))
  (Thread/sleep initial-sleep-time)
  (recur props))

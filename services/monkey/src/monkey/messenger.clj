(ns monkey.messenger
  "This namespace implements the Messages protocol where langhor is used to interface with an AMQP
   broker."
  (:gen-class)
  (:import [clojure.lang PersistentArrayMap]))


(defprotocol Messages
  "This protocol defines the operations need to interact with an AMQP broker.")


(deftype ^{:private true} Messenger []
  Messages)


(defn ^Messages mk-messenger
  "creates the object used to interact with the AMQP broker

   Parameters:
     props - The configuration properties map

   Returns:
     It returns the object."
  [^PersistentArrayMap props]
  (->Messenger))

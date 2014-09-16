(ns monkey.props
  "This namespace holds all of the logic for managing configuration values."
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import [java.net URL]
           [clojure.lang PersistentArrayMap]))


(def ^{:private true :const true} prop-names
  #{"monkey.amqp.host"
    "monkey.amqp.port"
    "monkey.amqp.user"
    "monkey.amqp.password"
    "monkey.amqp.queue"
    "monkey.amqp.exchange.name"
    "monkey.amqp.exchange.durable"
    "monkey.amqp.exchange.auto-delete"
    "monkey.es.url"
    "monkey.es.index"
    "monkey.es.tag-type"
    "monkey.es.batch-size"
    "monkey.es.scroll-size"
    "monkey.es.scroll-timeout"
    "monkey.log-progress-enabled"
    "monkey.log-progress-interval"
    "monkey.retry-period-ms"
    "monkey.tags.host"
    "monkey.tags.port"
    "monkey.tags.db"
    "monkey.tags.user"
    "monkey.tags.password"
    "monkey.tags.batch-size"})


(defn ^String amqp-host
  "Returns the hostname of the AMQP broker.

   Parameters:
     props - the property map to use

   Returns:
     the AMQP broker hostname"
  [^PersistentArrayMap props]
  (string/trim (get props "monkey.amqp.host")))


(defn ^Integer amqp-port
  "Returns the IP port of the AMQP broker listens on.

   Parameters:
     props - the property map to use

   Returns:
     the AMQP broker IP port"
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get props "monkey.amqp.port"))))


(defn ^String amqp-user
  "Returns the username of provided to the AMQP broker for authorization purposes.

   Parameters:
     props - the property map to use

   Returns:
     the authorized username"
  [^PersistentArrayMap props]
  (get props "monkey.amqp.user"))


(defn ^String amqp-password
  "Returns the password used to authenticate the username.

   Parameters:
     props - the property map to use

   Returns:
     the authentication password"
  [^PersistentArrayMap props]
  (get props "monkey.amqp.password"))


(defn ^String amqp-exchange-name
  "Returns the name AMQP exchange respondsible for routing messages to monkey.

   Parameters:
     props - the property map to use

   Returns:
     tue AMQP exchange name"
  [^PersistentArrayMap props]
  (get props "monkey.amqp.exchange.name"))


(defn ^Boolean amqp-exchange-durable?
  "Indicates whether or not the exchange is durable.

   Parameters:
     props - the property map to use

   Returns:
     true if the exchange is durable, otherwise false"
  [^PersistentArrayMap props]
  (Boolean/parseBoolean (string/trim (get props "monkey.amqp.exchange.durable"))))


(defn ^Boolean amqp-exchange-auto-delete?
  "Indicates whether or not broker auto delete's this exchange.

   Parameters:
     props - the property map to use

   Returns:
     true if the exchange is automatically deleted, otherwise false"
  [^PersistentArrayMap props]
  (Boolean/parseBoolean (string/trim (get props "monkey.amqp.exchange.auto-delete"))))


(defn ^String amqp-queue
  "Returns the AMQP queue name.

   Parameters:
     props - the property map to use

   Returns:
     the queue name"
  [^PersistentArrayMap props]
  (get props "monkey.amqp.queue"))


(defn ^Integer es-batch-size
  "Returns the indexing bulk operations batch size.

   Parameters:
     props - the property map to use

   Returns:
     the number of documents to handle at once in a bulk indexing operation"
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get props "monkey.es.batch-size"))))


(defn ^URL es-url
  "Returns the elasticsearch base URL.

   Parameters:
     props - The property map to use.

   Returns:
     It returns the elasticsearch base URL."
  [^PersistentArrayMap props]
  (URL. (string/trim (get props "monkey.es.url"))))


(defn ^String es-index
  "Returns the index in elasticsearch where the tags are indexed.

   Parameters:
     props - the property map to use

   Returns:
     the name of the index"
  [^PersistentArrayMap props]
  (get props "monkey.es.index"))


(defn ^String es-tag-type
  "returns the elasticsearch mapping type for a tag

   Parameters:
     props - the property map to use

   Returns:
     the tag mapping type"
  [^PersistentArrayMap props]
  (get props "monkey.es.tag-type"))


(defn ^Integer es-scroll-size
  "Returns the number of documents to retrieve at a time when scrolling through an elasticsearch
   result set.

   Parameters:
     props - the property map to use

   Returns:
     It returns the scroll size"
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get props "monkey.es.scroll-size"))))


(defn ^String es-scroll-timeout
  "Returns the unitted timeout value for a scroll.

   Parameters:
     props - the property map to use

   Returns:
     It returns the scroll timeout"
  [^PersistentArrayMap props]
  (string/trim (get props "monkey.es.scroll-timeout")))


(defn ^Boolean log-progress?
  "Indicates whether or not progress logging is enabled.

   Parameters:
     props - the property map to use

   Returns:
     It returns the true if progress should be logged, otherwise false."
  [^PersistentArrayMap props]
  (Boolean/parseBoolean (string/trim (get props "monkey.log-progress-enabled"))))


(defn ^Integer progress-logging-interval
  "It returns the number of items that must of been processed before progress is logged.

   Parameters:
     props - the property map to use

   Returns:
     It returns the item count."
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get props "monkey.log-progress-interval"))))


(defn ^Integer retry-period
  "It returns the amount of time to wait in milliseconds before retrying an operation.

   Parameters:
     props - the property map to use

   Returns:
     It return the period to wait."
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get props "monkey.retry-period-ms"))))


(defn ^String tags-host
  "Returns the tags database host

   Parameters:
     props - The property map to use.

   Returns:
     It returns the domain name of the host of the tags database."
  [^PersistentArrayMap props]
  (string/trim (get props "monkey.tags.host")))


(defn ^Integer tags-port
  "Returns the tags database port

   Parameters:
     props - The property map to use.

   Returns:
     It returns the port the tags database listens on."
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get props "monkey.tags.port"))))


(defn ^String tags-db
  "Returns the name of the tags database

   Parameters:
     props - the property map to use

   Returns:
     It returns the name of the tags database"
  [^PersistentArrayMap props]
  (get props "monkey.tags.db"))


(defn ^String tags-user
  "Returns the username authorized to access the tags database.

   Parameters:
     props - The properties map to use.

   Returns:
     It returns the authorized username."
   [^PersistentArrayMap props]
  (get props "monkey.tags.user"))


(defn ^String tags-password
  "returns the password used to authenticate the authorized user

   Parameters:
     props - the property map to use

   Returns:
     It returns the password"
  [^PersistentArrayMap props]
  (get props "monkey.tags.password"))


(defn ^Integer tags-batch-size
  "Returns the tags inspection bulk operations batch size.

   Parameters:
     props - the property map to use

   Returns:
     the number of tags to handle at once in a bulk inspection operation"
  [^PersistentArrayMap props]
  (Integer/parseInt (string/trim (get props "monkey.tags.batch-size"))))


(defn ^Boolean validate
  "Validates the configuration. We don't want short-circuit evaluation in this case because
   logging all missing configuration settings is helpful.

   Parameters:
     props - The property map to validate

   Returns:
     It returns true if all of the required parameters are present and false otherwise."
  [^PersistentArrayMap props]
  (let [missing-props (set/difference prop-names (set (keys props)))
        all-present   (empty? missing-props)]
    (when-not all-present
      (doseq [missing-prop missing-props]
        (log/error "configuration setting" missing-prop "is undefined")))
    all-present))

(ns conrad.database
  (:use [conrad.config])
  (:require [clojure.java.jdbc :as jdbc])
  (:import [java.sql Types]
           [javax.sql DataSource]
           [com.mchange.v2.c3p0 ComboPooledDataSource]))

(defn- get-required [props name msg]
  (let [value (get props name)]
    (if (nil? value)
      (throw (IllegalArgumentException. msg))
      value)))

(def drivers
  {"mysql" "com.mysql.jdbc.Driver"
   "postgresql" "org.postgresql.Driver"})

(defn- get-driver [vendor]
  (get-required drivers vendor (str "driver not known for " vendor)))

(def subprotocols
  {"mysql" "mysql"
   "postgresql" "postgresql"})

(defn- get-subprotocol [vendor]
  (get-required subprotocols vendor (str "subprotocol not known for " vendor)))

(defn db-spec
  "Constructs a database connection specification from the configuration
   settings."
  []
  {:classname (get-driver (db-vendor))
   :subprotocol (get-subprotocol (db-vendor))
   :subname (str "//" (db-host) ":" (db-port) "/" (db-name))
   :user (db-user)
   :password (db-password)
   :max-idle-time (db-max-idle-time)})

(defn- pool [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               (.setMaxIdleTime(:max-idle-time spec)))]
    {:datasource cpds}))

(def pooled-db (delay (pool (db-spec))))

(defn db-connection [] @pooled-db)

(defn get-column-metadata
  "Obtains the metadata for a table column."
  [table-name column-name]
  (first (filter
           #(= (:column_name %) column-name)
           (jdbc/resultset-seq
             (-> (jdbc/connection)
               (.getMetaData)
               (.getColumns nil nil table-name column-name))))))

(defn is-text-column
  "Determines whether or not a table column represents a text column."
  ([table-name column-name]
    (is-text-column (get-column-metadata table-name column-name)))
  ([{data-type :data_type}]
    (or (= data-type Types/CHAR)
        (= data-type Types/LONGNVARCHAR)
        (= data-type Types/LONGVARCHAR)
        (= data-type Types/VARCHAR))))

(defn validate-field-length
  "Validates a field value against the field length constraint in the
   database."
  [table-name column-name field-value]
  (let [column-metadata (get-column-metadata table-name column-name)
        max-len (Integer/parseInt (:char_octet_length column-metadata))]
    (when (and (is-text-column column-metadata)
             (> (count field-value) max-len))
      (throw (IllegalArgumentException.
               (str column-name " can be at most " max-len
                    " characters long"))))))

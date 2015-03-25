(ns donkey.util.db
  (:use [donkey.util.config]
        [korma.db]))

(defn- create-db-spec
  "Creates the database connection spec to use when accessing the database
   using Korma."
  []
  {:classname   (db-driver-class)
   :subprotocol (db-subprotocol)
   :subname     (str "//" (db-host) ":" (db-port) "/" (db-name))
   :user        (db-user)
   :password    (db-password)})

(defn- create-metadata-db-spec
  "Creates the database connection spec to use when accessing the metadata database using Korma."
  []
  {:classname   (metadata-db-driver-class)
   :subprotocol (metadata-db-subprotocol)
   :subname     (str "//" (metadata-db-host) ":" (metadata-db-port) "/" (metadata-db-name))
   :user        (metadata-db-user)
   :password    (metadata-db-password)})

(defn define-database
  "Defines the database connection to use from within Clojure."
  []
  (let [spec (create-db-spec)
        metadata-spec (create-metadata-db-spec)]
    (defonce de (create-db spec))
    (defonce metadata (create-db metadata-spec))
    (default-connection de)))

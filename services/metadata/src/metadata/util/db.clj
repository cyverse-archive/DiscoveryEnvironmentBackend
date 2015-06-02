(ns metadata.util.db
  (:use [metadata.util.config]
        [kameleon.db :only [define-metadata-database metadata]]
        [korma.db]))

(defn- create-db-spec
  "Creates the database connection spec to use when accessing the metadata database using Korma."
  []
  {:classname   (db-driver-class)
   :subprotocol (db-subprotocol)
   :subname     (str "//" (db-host) ":" (db-port) "/" (db-name))
   :user        (db-user)
   :password    (db-password)})

(defn define-database
  "Defines the database connection to use from within Clojure."
  []
  (define-metadata-database (create-db-spec))
  (default-connection metadata))

(ns data-info.util.db
  (:use [data-info.util.config]
        [kameleon.db :only [define-metadata-database]]
        [korma.db]))

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
  (define-metadata-database (create-metadata-db-spec)))

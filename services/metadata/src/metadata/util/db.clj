(ns metadata.util.db
  (:use [metadata.util.config]
        [korma.db]))

(defn- create-db-spec
  "Creates the database connection spec to use when accessing the metadata database using Korma."
  []
  (postgres
    {:classname   (db-driver-class)
     :subprotocol (db-subprotocol)
     :host        (db-host)
     :port        (db-port)
     :db          (db-name)
     :user        (db-user)
     :password    (db-password)}))

(defn define-database
  "Defines the database connection to use from within Clojure."
  []
  (defdb metadata (create-db-spec)))

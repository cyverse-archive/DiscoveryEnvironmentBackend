(ns job-migrator.db
  (:use [job-migrator.config]
        [korma.db])
  (:import [java.sql Timestamp]))

(defn- create-db-spec
  "Creates the database connection spec to use when accessing the database
   using Korma."
  []
  {:classname   (db-driver-class)
   :subprotocol (db-subprotocol)
   :subname     (str "//" (db-host) ":" (db-port) "/" (db-name))
   :user        (db-user)
   :password    (db-password)})

(defn define-database
  "Defines the database connection to use from within Clojure."
  []
  (let [spec (create-db-spec)]
    (defonce de (create-db spec))
    (default-connection de)))

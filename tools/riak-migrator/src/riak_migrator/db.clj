(ns riak-migrator.db
  (:use [kameleon.entities]
        [kameleon.core]
        [korma.core]
        [korma.db :only [default-connection]]
        [kameleon.pgpass]))

(defn subname
  [host port db]
  (str "//" host ":" port "/" db))

(defn db-spec
  [options]
  (let [db-host (:db-host options)
        db-port (:db-port options)
        db-user (:db-user options)
        db-name (:db-name options)]
    {:subprotocol "postgresql"
     :classname   "org.postgresql.Driver"
     :subname     (subname db-host db-port db-name)
     :user        db-user
     :password    (get-password db-host db-port db-name db-user)}))

(defn connect-db
  [options]
  (default-connection (db-spec options)))

(defn all-users
  []
  (mapv :username (select users (fields :username))))


(ns irods-avu-migrator.db
  (:use [kameleon.core]
        [kameleon.pgpass]
        [korma.core])
  (:require [korma.db :as db]))

(defn de-db-spec
  "Creates a Korma db spec for the DE database."
  [{:keys [db-host db-port db-user db-name]
    :or {db-port 5432
         db-name "de"}}]
  (db/postgres {:host     db-host
                :port     db-port
                :db       db-name
                :user     db-user
                :password (get-password db-host db-port db-name db-user)}))

(defn metadata-spec
  "Creates a Korma db spec for the Metadata database."
  [{:keys [db-host db-port db-user db-metadata-name]
    :or {db-port 5432
         db-metadata-name "metadata"}}]
  (db/postgres {:host     db-host
                :port     db-port
                :db       db-metadata-name
                :user     db-user
                :password (get-password db-host db-port db-metadata-name db-user)}))

(defn icat-db-spec
  "Creates a Korma db spec for the ICAT."
  [{:keys [icat-host icat-port icat-user icat-name]
    :or {icat-port 5432
         icat-name "ICAT"}}]
  (db/postgres {:host     icat-host
                :port     icat-port
                :db       icat-name
                :user     icat-user
                :password (get-password icat-host icat-port icat-name icat-user)}))

(defn connect-de-db
  [options]
  (db/defdb de (de-db-spec options)))

(defn connect-metadata-db
  [options]
  (db/defdb metadata (metadata-spec options)))

(defn connect-icat
  [options]
  (db/defdb icat (icat-db-spec options)))

(defn connect-dbs
  [options]
  (connect-icat options)
  (connect-metadata-db options)
  (connect-de-db options)
  (db/default-connection de))

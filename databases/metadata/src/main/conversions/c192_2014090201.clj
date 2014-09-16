(ns facepalm.c192-2014090201
  (:require [clojure.java.jdbc :as jdbc]
            [korma.core :as sql]
            [korma.db :as db]
            [clj-jargon.init :as fs-init]
            [clj-jargon.metadata :as fs-meta]))


(def ^{:private true} version
  "The destination database version."
  "1.9.2:20140902.01")


;; This statement can't be run inside a transaction. That's why jdbc is directly used.
(defn- create-folder-type
  [admin-db-spec]
  (println "\t* creating the folder target enum value")
  (jdbc/db-do-prepared (db/get-connection admin-db-spec)
                       false
                       "ALTER TYPE target_enum ADD VALUE 'folder' AFTER 'data'"))


(defn- rename-data-type
  [admin-db-spec]
  (println "\t* renaming data target enum value to file")
  (db/with-db admin-db-spec
    (sql/exec-raw "UPDATE pg_enum
                     SET enumlabel = 'file'
                     WHERE enumlabel = 'data'
                       AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'target_enum')")))


(defn- convert-to-folder
  [table tgt]
  (sql/update table
    (sql/set-fields {:target_type (sql/raw "'folder'")})
    (sql/where {:target_id tgt})))


(defn- folder?
  [fs tgt]
  (not (empty? (fs-meta/list-collections-with-attr-value fs "ipc_UUID" tgt))))


(defn- get-data-targets
  [table]
  (sql/select table (sql/fields :target_id)
    (sql/where {:target_type (sql/raw "'data'")})))


(defn- update-table
  [fs table]
  (println "\t* updating the target types for" (name table))
  (doseq [tgt (get-data-targets table)]
    (when (folder? fs tgt)
      (convert-to-folder table tgt))))


(def convert
  "Performs the conversion for database version 1.9.2:20140902.01"
  ^{:cfg-file "donkey.properties"}
  (fn [admin-db-spec donkey-cfg]
    (println "Performing the conversion for" version)
    (let [fs-cfg (fs-init/init (get donkey-cfg "donkey.irods.host")
                               (get donkey-cfg "donkey.irods.port")
                               (get donkey-cfg "donkey.irods.user")
                               (get donkey-cfg "donkey.irods.pass")
                               (get donkey-cfg "donkey.irods.home")
                               (get donkey-cfg "donkey.irods.zone")
                               (get donkey-cfg "donkey.irods.resc"))]
      (fs-init/with-jargon fs-cfg [fs]
        (create-folder-type admin-db-spec)
        (update-table fs :attached_tags)
        (update-table fs :avus)
        (update-table fs :comments)
        (update-table fs :favorites)
        (update-table fs :file_links)
        (update-table fs :ratings)
        (rename-data-type admin-db-spec)))))

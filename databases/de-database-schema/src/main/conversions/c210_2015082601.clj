(ns facepalm.c210-2015082601
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150826.01")

(defn- rename-columns
  []
  (exec-raw "ALTER TABLE ONLY container_volumes_from
             RENAME COLUMN data_container_id TO data_containers_id;")

  (exec-raw "ALTER TABLE ONLY data_containers
             RENAME COLUMN container_image_id TO container_images_id;")

  (exec-raw "ALTER TABLE ONLY data_container_volumes
             RENAME COLUMN data_container_id TO data_containers_id;"))

(defn convert
  "Performs the conversion for database version 2.1.0:20150826.01"
  []
  (println "Performing the conversion for" version)
  (rename-columns))

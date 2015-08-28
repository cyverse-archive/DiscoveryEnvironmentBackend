(ns facepalm.c210-2015082801
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150828.01")

(defn column?
  "Returns true if the column exists. Parameter should be in the format
  'table-name.column-name'."
  [column]
  (let [[table-name column-name] (clojure.string/split column #"\.")]
    (pos?
     (count
      (select :information_schema.columns
              (fields :column_name)
              (where {:table_name  [= table-name]
                      :column_name [= column-name]}))))))

(defn constraint?
  "Returns true if the constraint exists. Parameter should be in the format
  'table-name.constraint-name'."
  [constraint]
  (let [[table-name constraint-name] (clojure.string/split constraint #"\.")]
    (pos?
     (count
      (select :information_schema.table_constraints
              (fields :constraint_name)
              (where {:table_name      [= table-name]
                      :constraint_name [= constraint-name]}))))))

(defn- fix-data-containers-columns
  []
  (when (column? "container_volumes_from.data_container_id")
    (println "\t* Renaming container_volumes_from.data_container_id to container_volumes_from.data_containers_id")
    (exec-raw "ALTER TABLE ONLY container_volumes_from
               RENAME COLUMN data_container_id TO data_containers_id;"))

  (when (constraint? "container_volumes_from.container_volumes_from_data_container_id_fkey")
    (println "\t* Renaming container_volumes_from_data_container_id_fkey to \n\t  container_volumes_from_data_containers_id_fkey")
    (exec-raw "ALTER TABLE ONLY container_volumes_from
               RENAME CONSTRAINT container_volumes_from_data_container_id_fkey TO container_volumes_from_data_containers_id_fkey;"))

  (when (column? "data_containers.container_image_id")
    (println "\t* Renaming data_containers.container_image_id to data_containers.container_images_id")
    (exec-raw "ALTER TABLE ONLY data_containers
               RENAME COLUMN container_image_id TO container_images_id;"))

  (when (constraint? "data_containers.data_containers_container_image_id_fkey")
    (println "\t* Renaming data_containers_container_image_id_fkey to \n\t  data_containers_container_images_id_fkey")
    (exec-raw "ALTER TABLE ONLY data_containers
               RENAME CONSTRAINT data_containers_container_image_id_fkey TO data_containers_container_images_id_fkey;"))

  (when (column? "data_container_volumes.data_container_id")
    (println "\t* Renaming data_container_volumes.data_container_id to data_container_volumes.data_containers_id")
    (exec-raw "ALTER TABLE ONLY data_container_volumes
               RENAME COLUMN data_container_id TO data_containers_id;"))

  (when (constraint? "data_container_volumes.data_container_volumes_data_container_id_fkey")
    (println "\t* Renaming data_container_volumes_data_container_id_fkey to \n\t  data_container_volumes_data_containers_id_fkey")
    (exec-raw "ALTER TABLE ONLY data_container_volumes
               RENAME CONSTRAINT data_container_volumes_data_container_id_fkey TO data_container_volumes_data_containers_id_fkey;")))

(defn convert
  "Performs the conversion for database version 2.1.0:20150828.01"
  []
  (println "Performing the conversion for" version)
  (fix-data-containers-columns))

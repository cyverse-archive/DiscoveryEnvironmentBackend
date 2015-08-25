(ns facepalm.c210-2015082501
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150825.01")

(defn- add-data-containers-table
  []
  (println "\t* adding data_containers table")
  (load-sql-file "tables/75_data_containers.sql")
  (load-sql-file "constraints/75_data_containers.sql"))

(defn- add-data-container-volumes-table
  []
  (println "\t* adding data_container_volumes table")
  (load-sql-file "tables/76_data_container_volumes.sql")
  (load-sql-file "constraints/76_data_container_volumes.sql"))

(defn convert
  "Performs the conversion for database version 2.1.0:20150825.01"
  []
  (println "Performing the conversion for" version)
  (add-data-containers-table)
  (add-data-container-volumes-table))

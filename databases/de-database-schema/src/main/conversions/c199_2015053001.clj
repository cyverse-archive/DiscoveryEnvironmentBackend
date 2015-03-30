(ns facepalm.c199-2015053001
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "1.9.9:20150530.01")

(defn- add-container-images-table
  []
  (println "\t* adding container_images table")
  (load-sql-file "tables/70_container_images.sql")
  (load-sql-file "constraints/70_container_images.sql"))

(defn- add-container-settings-table
  []
  (println "\t* adding container_settings table")
  (load-sql-file "tables/71_container_settings.sql")
  (load-sql-file "constraints/71_container_settings.sql"))

(defn- add-container-devices-table
  []
  (println "\t* adding container_devices table")
  (load-sql-file "tables/72_container_devices.sql")
  (load-sql-file "constraints/72_container_devices.sql"))

(defn- add-container-volumes-table
  []
  (println "\t* adding container_volumes table")
  (load-sql-file "tables/73_container_volumes.sql")
  (load-sql-file "constraints/73_container_volumes.sql"))

(defn- add-container-volumes-from-table
  []
  (println "\t* adding container_volumes_from table")
  (load-sql-file "tables/74_container_volumes_from.sql")
  (load-sql-file "constraints/74_container_volumes_from.sql"))

(defn convert
  "Performs the conversion for database version 1.9.7:20150530.01"
  []
  (println "Performing the conversion for" version)
  (add-container-images-table)
  (add-container-settings-table)
  (add-container-devices-table)
  (add-container-volumes-table)
  (add-container-volumes-from-table))

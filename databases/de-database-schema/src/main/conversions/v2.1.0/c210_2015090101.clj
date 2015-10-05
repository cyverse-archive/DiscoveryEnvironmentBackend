(ns facepalm.c210-2015090101
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150901.01")

(defn- add-data-containers
  []
  (println "\t* Adding a data container to DE Word Count")
  (exec-raw "INSERT INTO data_containers (id, name_prefix, container_images_id)
             VALUES ('115584ad-7bc3-4601-89a2-85a4e5b5f6a4', 'wc-data', '15959300-b972-4571-ace2-081af0909599');")

  (println "\t *Adding a volume from for DE Word Count")
  (exec-raw "INSERT INTO container_volumes_from (data_containers_id, container_settings_id)
               SELECT '115584ad-7bc3-4601-89a2-85a4e5b5f6a4',
                      container_settings.id
                 FROM container_settings
                WHERE container_settings.tools_id = '85cf7a33-386b-46fe-87c7-8c9d59972624'
                  AND container_settings.network_mode = 'none'
                  AND container_settings.entrypoint = 'wc'
                LIMIT 1;"))

(defn convert
  "Performs the conversion for database version 2.1.0:20150901.01"
  []
  (println "Performing the conversion for" version)
  (add-data-containers))

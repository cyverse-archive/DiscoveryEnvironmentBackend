(ns facepalm.c210-2015082503
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150825.03")

(defn- get-data-container-id
  [name-prefix]
  (:id (first
         (select :data_containers
                 (fields :id)
                 (where {:name_prefix name-prefix})))))

(defn- fix-container-volumes-from
  []
  ;;; add data_containers_id column (without constraints)
  (exec-raw "ALTER TABLE ONLY container_volumes_from
             ADD COLUMN data_containers_id uuid;")

  ;;; get the data_containers_id column for the ncbi stuff
  (let [ssh-key-id (get-data-container-id "ncbi-sra-submit-ssh-key-data")
        configs-id (get-data-container-id "ncbi-sra-configs")
        test-configs-id (get-data-container-id "ncbi-sra-test-configs")]
    ;;; add the ncbi data_container_ids where the name matches the name_prefix
    (update :container_volumes_from
      (set-fields {:data_containers_id ssh-key-id})
      (where {:name "ncbi-sra-submit-ssh-key-data"}))
    (update :container_volumes_from
      (set-fields {:data_containers_id configs-id})
      (where {:name "ncbi-sra-configs"}))
    (update :container_volumes_from
      (set-fields {:data_containers_id test-configs-id})
      (where {:name "ncbi-sra-test-configs"}))

    ;;; delete all rows that don't have a data_container_ids
    (delete :container_volumes_from
      (where {:data_containers_id [= nil]}))

    ;;; drop the name column
    (exec-raw "ALTER TABLE ONLY container_volumes_from
               DROP COLUMN name;")

    ;;; add the not null constraint on the data_containers_id column
    (exec-raw "ALTER TABLE ONLY container_volumes_from
               ALTER COLUMN data_containers_id SET NOT NULL;")

    ;;; add the foreign key constraint
    (exec-raw "ALTER TABLE ONLY container_volumes_from
               ADD CONSTRAINT container_volumes_from_data_containers_id_fkey
               FOREIGN KEY(data_containers_id)
               REFERENCES data_containers(id);")))

(defn convert
  "Performs the conversion for database version 2.1.0:20150825.03"
  []
  (println "Performing the conversion for" version)
  (fix-container-volumes-from))

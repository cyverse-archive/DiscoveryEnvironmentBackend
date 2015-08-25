(ns facepalm.c210-2015082503
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150825.03")

(defn- fix-container-volumes-from
  []
  ;;; add data_containers_id column (without constraints)
  (exec-raw "ALTER TABLE ONLY container_volumes_from
             ADD COLUMN data_container_id uuid;")

  ;;; get the data_container_id column for the ncbi stuff
  (let [ssh-key-id (first (map :id (select :data_containers (fields [:id]) (where (= :name_prefix "ncbi-ssh-key")))))
        configs-id (first (map :id (select :data_containers (fields [:id]) (where (= :name_prefix "ncbi-sra-configs")))))]
    ;;; add the ncbi data_container_ids where the name matches the name_prefix
    (update :container_volumes_from
      (set-fields {:data_container_id ssh-key-id})
      (where {:name [= "ncbi-ssh-key"]}))
    (update :container_volumes_from
      (set-fields {:data_container_id configs-id})
      (where {:name [= "ncbi-sra-configs"]}))

    ;;; delete all rows that don't have a data_container_ids
    (delete :container_volumes_from
      (where {:data_container_id [= nil]}))

    ;;; drop the name column
    (exec-raw "ALTER TABLE ONLY container_volumes_from
               DROP COLUMN name;")

    ;;; add the not null constraint on the data_container_id column
    (exec-raw "ALTER TABLE ONLY container_volumes_from
               ALTER COLUMN data_container_id SET NOT NULL;")

    ;;; add the foreign key constraint
    (exec-raw "ALTER TABLE ONLY container_volumes_from
               ADD CONSTRAINT container_volumes_from_data_container_id_fkey
               FOREIGN KEY(data_container_id)
               REFERENCES data_containers(id);")
               
    (insert :version (values {:version version}))))

(defn convert
  "Performs the conversion for database version 2.1.0:20150825.03"
  []
  (println "Performing the conversion for" version)
  (fix-container-volumes-from))

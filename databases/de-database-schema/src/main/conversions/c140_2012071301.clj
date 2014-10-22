(ns facepalm.c140-2012071301
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.4.0:20120713.01")

(defn- add-data-source-table
  "Adds the data source table to the database."
  []
  (println "\t* adding the data_source table")
  (exec-raw "CREATE SEQUENCE data_source_id_seq")
  (exec-raw
   "CREATE TABLE data_source (
        id bigint DEFAULT nextval('data_source_id_seq'),
        uuid char(36) NOT NULL,
        name varchar(50) UNIQUE NOT NULL,
        label varchar(50) NOT NULL,
        description varchar(255) NOT NULL,
        PRIMARY KEY(id)
    )"))

(defn- add-data-source-id-to-dataobjects-table
  "Adds the data_source_id column to the dataobjects table."
  []
  (println "\t* adding the data_source_id to the dataobjects table")
  (exec-raw
   "ALTER TABLE dataobjects
    ADD COLUMN data_source_id bigint")
  (exec-raw
   "ALTER TABLE ONLY dataobjects
    ADD CONSTRAINT dataobjects_data_source_id_fkey
    FOREIGN KEY (data_source_id)
    REFERENCES data_source(id)"))

(defn- populate-data-source-table
  "Populates the data source table."
  []
  (println "\t* populating the data_source table")
  (insert :data_source
          (values [{:uuid        "8D6B8247-F1E7-49DB-9FFE-13EAD7C1AED6"
                    :name        "file"
                    :label       "File"
                    :description "A regular file."}
                   {:uuid        "1EEECF26-367A-4038-8D19-93EA80741DF2"
                    :name        "stdout"
                    :label       "Standard Output"
                    :description "Redirected standard output from a job."}
                   {:uuid        "BC4CF23F-18B9-4466-AF54-9D40F0E2F6B5"
                    :name        "stderr"
                    :label       "Standard Error Output"
                    :description "Redirected error output from a job."}])))

(defn- get-data-source-id
  "Gets the internal data source identifier for the given UUID."
  [uuid]
  (first (map :id (select :data_source (where {:uuid uuid})))))

(defn- initialize-data-source-for-existing-data-objects
  "Initializes the data source for all existing data objects.  Only regular
   files were supported up to this point, so the data source for all existing
   data objects will be set to a regular file."
  []
  (println "\t* initializing the data source for existing data objects")
  (let [data-source (get-data-source-id "8D6B8247-F1E7-49DB-9FFE-13EAD7C1AED6")]
    (update :data_object (set-fields {:data_source_id data-source}))))

(defn convert
  "Performs the conversions for database version 1.40:20120713.01."
  []
  (println "Performing conversion for" version)
  (add-data-source-table)
  (add-data-source-id-to-dataobjects-table)
  (populate-data-source-table)
  (initialize-data-source-for-existing-data-objects))

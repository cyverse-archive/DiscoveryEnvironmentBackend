(ns facepalm.c196-2015031001
  (:use [korma.core]
        [kameleon.sql-reader :only [exec-sql-statement]]))

(def ^:private version
  "The destination database version."
  "1.9.6:20150310.01")

(defn- fix-reference-type-file-parameters
  "CORE-6569: ReferenceGenome, ReferenceSequence, and ReferenceAnnotation parameter types added
   since v1.9.3 are missing entries in the file_parameters table."
  []
  (println "\t* adding missing file_parameters entries for Reference Genome parameter types")
  (exec-sql-statement
    "INSERT INTO file_parameters(parameter_id, info_type, data_format, data_source_id)
    (SELECT id AS parameter_id,
            (SELECT id FROM info_type WHERE name = parameter_type) AS info_type,
            (SELECT id FROM data_formats WHERE name = 'Unspecified') AS data_format,
            (SELECT id FROM data_source WHERE name = 'file') AS data_source_id
      FROM task_param_listing t
      WHERE parameter_type LIKE 'Reference%'
        AND data_format IS NULL);"))

(defn convert
  "Performs the conversion for this version of the database"
  []
  (println "Performing the conversion for" version)
  (fix-reference-type-file-parameters))

(ns facepalm.c197-2015040201
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]
        [kameleon.uuids :only [uuidify]]))

(def ^:private version
  "The destination database version."
  "1.9.7:20150402.01")

(defn- add-metadata-template-attr-enum-type
  "CORE-6599: Add Metadata Template Attributes Enum type and Enum Values table."
  []
  (println "\t* adding Metadata Template Attributes Enum type")
  (insert :metadata_value_types (values {:id   (uuidify "B17ED53D-2B10-428F-B38A-C9DEC3DC5127")
                                         :name "Enum"}))
  (println "\t* adding metadata_attr_enum_values table")
  (load-sql-file "tables/62_metadata_attr_enum_values.sql")
  (println "\t* adding metadata_attr_enum_values table constraints")
  (load-sql-file "constraints/62_metadata_attr_enum_values.sql"))

(defn convert
  "Performs the conversion for this version of the database"
  []
  (println "Performing the conversion for" version)
  (add-metadata-template-attr-enum-type))

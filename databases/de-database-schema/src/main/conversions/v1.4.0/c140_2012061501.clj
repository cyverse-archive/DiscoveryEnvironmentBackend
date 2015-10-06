(ns facepalm.c140-2012061501
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.4.0:20120615.01")

(defn- trim-transformation-steps-columns
  "Trims leading and trailing whitespace from the 'name', 'guid' and
   'description' columns in the transformation_steps table."
  []
  (println "\t* trimming fields in transformation_steps table")
  (update :transformation_steps
          (set-fields {:name        (sqlfn trim :name)
                       :guid        (sqlfn trim :guid)
                       :description (sqlfn trim :description)})))

(defn convert
  "Performs the conversions for database version 1.40:20120615.01."
  []
  (println "Performing conversion for" version)
  (trim-transformation-steps-columns))

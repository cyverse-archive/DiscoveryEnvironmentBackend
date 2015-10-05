(ns facepalm.c200-2015052901
  (:use [korma.core]
        [kameleon.sql-reader :only [exec-sql-statement]]))

(def ^:private version
  "The destination database version."
  "2.0.0:20150529.01")

(defn- delete-metadata-complete-template-attr
  []
  (println "\t* deleting 'Metadata complete' Template attributes")
  (delete :metadata_template_attrs
    (where {:attribute_id [in (subselect :metadata_attributes
                                         (fields :id)
                                         (where {:name "Metadata complete"}))]}))
  (delete :metadata_attributes (where {:name "Metadata complete"})))

(defn convert
  "Performs the conversion for this version of the database"
  []
  (println "Performing the conversion for" version)
  (delete-metadata-complete-template-attr))

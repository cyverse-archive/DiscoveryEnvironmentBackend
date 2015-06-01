(ns facepalm.c200-2015052901
  (:use [korma.core]
        [kameleon.sql-reader :only [exec-sql-statement]]))

(def ^:private version
  "The destination database version."
  "2.0.0:20150529.01")

(defn- metadata-complete-avu-subselect
  []
  (subselect :avus
             (fields :id)
             (where {:attribute "Metadata complete"})))

(defn- delete-metadata-complete-template-instance-records
  []
  (println "\t* deleting the template instance records fro 'Metadata complete' AVUs")
  (delete :template_instances
          (where {:avu_id [in (metadata-complete-avu-subselect)]})))

(defn- delete-metadata-complete-template-attr
  []
  (println "\t* deleting 'Metadata complete' AVUs")
  (delete :avus (where {:attribute "Metadata complete"})))

(defn convert
  "Performs the conversion for this version of the database"
  []
  (println "Performing the conversion for" version)
  (delete-metadata-complete-template-instance-records)
  (delete-metadata-complete-template-attr))

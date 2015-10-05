(ns facepalm.c184-2014030401
 (:use [korma.core]))

(def ^:private version
 "1.8.4:20140304.01")

(defn- update-template-name
 []
 (println "\t* updating template name")
 (update :metadata_templates
  (set-fields {:name "iPlant Data Store Genome Sequence"})
  (where {:name "iDS Genome Sequences"})))

(defn convert
 []
 (println "Performing the conversion for" version)
 (update-template-name))

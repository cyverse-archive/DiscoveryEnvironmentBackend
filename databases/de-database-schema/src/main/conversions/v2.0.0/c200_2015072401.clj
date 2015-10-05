(ns facepalm.c200-2015072401
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "2.0.0:20150724.01")

(defn backwards-compat-new-registry
  []
  (println "\t* Changing the registry for the backwards-compat container")
  (exec-raw "UPDATE ONLY container_images
                SET \"name\" = 'gims.iplantcollaborative.org:5000/backwards-compat'
              WHERE id = 'fc210a84-f7cd-4067-939c-a68ec3e3bd2b';"))

(defn convert
  []
  (println "Performing the conversion for" version)
  (backwards-compat-new-registry))

(ns facepalm.c193-2015010601
  (:use korma.core))

(def ^:private version
  "The destination database version."
  "1.9.3:20150106.01")

(defn fix-negative-parameter-ordering
  []
  (println "\t* fixing negative parameter ordering...")
  (exec-raw
    "UPDATE parameters SET ordering = 0 WHERE id IN
     (SELECT id FROM task_param_listing p
      WHERE p.ordering < 0
      AND CHAR_LENGTH(p.name) > 0
      AND value_type != 'Boolean')"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (fix-negative-parameter-ordering))

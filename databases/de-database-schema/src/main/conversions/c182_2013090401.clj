(ns facepalm.c182-2013090401
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.2:20130904.01")

(defn- replace-app-count-function
  "Replaces the app_count function with a fix for the same app in multiple groups."
  []
  (println "\t* replacing the function, 'app_count'")
  (exec-raw
    "CREATE OR REPLACE FUNCTION app_count(bigint) RETURNS bigint AS $$
        SELECT COUNT(DISTINCT a.hid) FROM transformation_activity a
        JOIN template_group_template tgt on a.hid = tgt.template_id
        WHERE NOT a.deleted
        AND tgt.template_group_id in (SELECT * FROM app_group_hierarchy_ids($1))
    $$ LANGUAGE SQL"))

(defn convert
  "Performs the conversion for database version 1.8.2:20130904.01."
  []
  (println "Performing conversion for" version)
  (replace-app-count-function))

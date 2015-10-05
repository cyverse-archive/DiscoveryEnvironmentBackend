(ns facepalm.c160-2012110501
  (:use [korma.core]
        [kameleon.core])
  (:require [clojure.string :as string]))

(def ^:private version
  "The destination database version."
  "1.6.0:20121105.01")

(defn rename-app-groups
  "Renames any app groups containing the word, 'applications.'  This function uses an SQL function
   that is specific to PostgreSQL, so it may have to be rewritten if we switch to a different DBMS.
   We could do this by loading all of the results into memory and updating them one at a time, but
   that's quite a bit slower."
  []
  (println "\t* renaming app groups containing the word \"applications.\"")
  (update :template_group
          (set-fields {:name (sqlfn regexp_replace :name "Applications" "Apps")})
          (where {:name [like "%Applications%"]})))

(defn convert
  "Performs the conversions for database version 1.6.0:20121105.01."
  []
  (println "Performing the conversion for" version)
  (rename-app-groups))

(ns metadactyl.service.apps.de.jobs.util
  (:require [clojure.string :as string]))

(def not-blank? (comp (complement string/blank?) str))

(def input-multiplicities
  {"FileInput"         "single"
   "FolderInput"       "collection"
   "MultiFileSelector" "many"})

(def input-types
  (set (keys input-multiplicities)))

(def output-multiplicities
  {"FileOutput"      "single"
   "FolderOutput"    "collection"
   "MultiFileOutput" "many"})

(def output-types
  (set (keys output-multiplicities)))

(def environment-variable-type
  "EnvironmentVariable")

(def ignored-param-types
  #{environment-variable-type "Info"})

(defn ignored-param?
  [{:keys [type order]}]
  (or (contains? ignored-param-types type) (< order 0)))

(defn qual-id
  [step-id param-id]
  (str step-id "_" param-id))

(defn param->qual-id
  [param]
  (qual-id (:step_id param) (:id param)))

(def param->qual-key (comp keyword param->qual-id))

(defn input?
  [{:keys [type]}]
  (input-types type))

(defn output?
  [{:keys [type]}]
  (output-types type))

(defn executable?
  [component-type]
  (= component-type "executable"))

(defn fapi-app?
  [{job-type :overall_job_type}]
  (= job-type "fAPI"))

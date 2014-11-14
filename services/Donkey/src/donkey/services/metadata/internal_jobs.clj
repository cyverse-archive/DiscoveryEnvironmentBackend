(ns donkey.services.metadata.internal-jobs
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [donkey.util.config :as config]))

(defn- load-param-map
  [app-lister app-id]
  (->> (.getApp app-lister app-id)
       (:groups)
       (mapcat :parameters)
       (map (juxt :label :id))
       (into {})))

;; TODO: find a better way to associate parameter values with parameters.
(defn- build-config
  "Builds the configuration for an internal job submission. The first argument, param-map, is
   the parameter map returned by load-param-map above. It maps the parameter label to the
   parameter ID. The second argument, label-map, is another map that maps the parameter keyword
   used internally to the parameter label found in the app. The last argument, params, maps the
   keyword used internally to the parameter value."
  [param-map label-map params]
  (into {} (map (juxt (comp param-map label-map key) val) params)))

(defn- build-app-config
  [app-lister app-id label-map params]
  (build-config (load-param-map app-lister app-id) label-map params))

(def ^:private url-import-label-map
  {:address  "Source URL"
   :filename "Output Filename"})

(defn- avu
  [attr value unit]
  {:attr attr :value value :unit unit})

(defn- build-url-import-config
  [app-lister address filename]
  (->> {:address address :filename filename}
       (build-app-config app-lister (config/fileio-url-import-app) url-import-label-map)))

(defn- build-url-import-job-submission
  [app-lister address filename dest-path]
  {:config               (build-url-import-config app-lister address filename)
   :description          (str "URL Import of " filename " from " address)
   :name                 (str "url_import_" filename)
   :app_id               (str (config/fileio-url-import-app))
   :debug                false
   :create_output_subdir false
   :output_dir           dest-path
   :notify               true
   :skip-parent-meta     true
   :file-metadata        [(avu "ipc-url-import" address "Import URL")]
   :archive_logs         false})

(defn- launch-url-import-job
  [app-lister address filename dest-path]
  (->> (build-url-import-job-submission app-lister address filename dest-path)
       (.submitJob app-lister)))

(defn- unknown-job-type
  [job-type]
  (throw+ {:error_code ce/ERR_CONFIG_INVALID
           :reason     (str "unknown job type: " job-type)}))

(defn- get-submitter
  [job-type]
  (cond (= job-type :url-import) launch-url-import-job
        :else                    (unknown-job-type job-type)))

(defn submit
  [job-type agave params]
  (apply (get-submitter job-type) agave params))

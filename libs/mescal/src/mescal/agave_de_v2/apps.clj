(ns mescal.agave-de-v2.apps
  (:use [mescal.agave-de-v2.app-listings :only [get-app-name]])
  (:require [clojure.string :as string]
            [mescal.agave-de-v2.constants :as c]
            [mescal.agave-de-v2.params :as mp]
            [mescal.util :as util]))

(defn- get-boolean
  [value default]
  (cond (nil? value)    default
        (string? value) (Boolean/parseBoolean value)
        :else           value))

(defn- format-group
  [name params]
  (when (some :isVisible params)
    {:step_number 1
     :id          name
     :name        name
     :label       name
     :parameters  params}))

(defn- format-input-validator
  [input]
  {:required (get-boolean (get-in input [:value :required]) false)})

(defn- format-param
  [get-type get-value get-args param]
  {:description  (get-in param [:details :description])
   :arguments    (get-args param)
   :defaultValue (get-value param)
   :id           (:id param)
   :isVisible    (get-boolean (get-in param [:value :visible]) false)
   :label        (get-in param [:details :label])
   :name         (:id param)
   :order        0
   :required     (get-boolean (get-in param [:value :required]) false)
   :type         (get-type param)
   :validators   []})

(defn- param-formatter
  [get-type get-value get-args]
  (fn [param]
    (format-param get-type get-value get-args param)))

(defn- get-default-enum-value
  [{{enum-values :enum_values default :default} :value :as param}]
  (let [default (first default)]
    (when-let [default-elem (mp/find-enum-element default enum-values)]
      (mp/format-enum-element default default-elem))))

(defn- get-default-param-value
  [param]
  (if (mp/enum-param? param)
    (get-default-enum-value param)
    (get-in param [:value :default])))

(defn- get-param-args
  [{{enum-values :enum_values default :default} :value :as param}]
  (if (mp/enum-param? param)
    (map (partial mp/format-enum-element (first default)) enum-values)
    []))

(defn- input-param-formatter
  [& {:keys [get-default] :or {get-default (constantly "")}}]
  (param-formatter (constantly "FileInput") get-default (constantly [])))

(defn- opt-param-formatter
  [& {:keys [get-default] :or {get-default get-default-param-value}}]
  (param-formatter mp/get-param-type get-default get-param-args))

(defn- output-param-formatter
  [& {:keys [get-default] :or {get-default get-default-param-value}}]
  (param-formatter (constantly "FileOutput") get-default (constantly [])))

(defn- format-params
  [formatter-fn params]
  (map formatter-fn (sort-by #(get-in % [:value :order] 0) params)))

(defn format-groups
  [app]
  (remove nil?
          [(format-group "Inputs" (format-params (input-param-formatter) (:inputs app)))
           (format-group "Parameters" (format-params (opt-param-formatter) (:parameters app)))
           (format-group "Outputs" (format-params (output-param-formatter) (:outputs app)))]))

(defn system-available?
  [agave system-name]
  (not= "UP" (:status (.getSystemInfo agave system-name))))

(defn format-app
  ([agave app group-format-fn]
     (let [system-name (:executionSystem app)
           app-label   (get-app-name app)
           mod-time    (util/parse-timestamp (:lastModified app))]
       {:groups           (group-format-fn app)
        :disabled         (system-available? agave system-name)
        :label            app-label
        :id               (:id app)
        :name             app-label
        :description      (:shortDescription app)
        :integration_date mod-time
        :edited_date      mod-time}))
  ([agave app]
     (format-app agave app format-groups)))

(defn load-app-info
  [agave app-ids]
  (->> (.listApps agave)
       (filter (comp (set app-ids) :id))
       (map (juxt :id identity))
       (into {})))

(defn format-deployed-component-for-app
  [{path :deploymentPath :as app}]
  {:attribution ""
   :description (:shortDescription app)
   :id          (:id app)
   :location    (string/replace path #"/[^/]+$" "")
   :name        (string/replace path #"^.*/" "")
   :type        (:executionType app)
   :version     (:version app)})

(defn format-app-details
  [app]
  (let [app-label (get-app-name app)
        mod-time  (str (util/parse-timestamp (:lastModified app)))]
    {:published_date   mod-time
     :edited_date      mod-time
     :id               (:id app)
     :references       []
     :description      (:shortDescription app)
     :label            app-label
     :tito             (:id app)
     :components       [(format-deployed-component-for-app app)]
     :groups           [c/hpc-group-overview]
     :suggested_groups [c/hpc-group-overview]}))

(defn- add-file-info
  [prop]
  (assoc prop
    :format         "Unspecified"
    :retain         false
    :file_info_type "File"))

(defn format-app-tasks
  [app]
  (let [app-name        (get-app-name app)
        select-io-keys  #(select-keys % [:description :format :id :label :name :required])
        format-io-field (comp select-io-keys add-file-info)
        inputs          (map (comp format-io-field (input-param-formatter)) (:inputs app))
        outputs         (map (comp format-io-field (output-param-formatter)) (:outputs app))]
    {:description (:shortDescription app)
     :id          (:id app)
     :name        (get-app-name app)
     :tasks       [{:description (:shortDescription app)
                    :id          (:id app)
                    :inputs      inputs
                    :name        app-name
                    :outputs     outputs}]}))

(defn- format-rerun-value
  [p v]
  (when p
    (if (mp/enum-param? p)
      (mp/format-enum-element v (mp/find-enum-element v (get-in p [:value :enum_values])))
      v)))

(defn- app-rerun-value-getter
  [job k]
  (let [values (job k)]
    (fn [p]
      (or (format-rerun-value p (values (keyword (:id p))))
          (get-default-param-value p)))))

(defn- format-groups-for-rerun
  [agave job app]
  (let [input-getter (comp #(.irodsFilePath agave %) (app-rerun-value-getter job :inputs))
        format-input (input-param-formatter :get-default input-getter)
        opt-getter   (app-rerun-value-getter job :parameters)
        format-opt   (opt-param-formatter :get-default opt-getter)]
    (remove nil?
            [(format-group "Inputs" (map format-input (:inputs app)))
             (format-group "Parameters" (map format-opt (:parameters app)))
             (format-group "Outputs" (map (output-param-formatter) (:outputs app)))])))

(defn format-app-rerun-info
  [agave app job]
  (format-app app (partial format-groups-for-rerun agave job)))

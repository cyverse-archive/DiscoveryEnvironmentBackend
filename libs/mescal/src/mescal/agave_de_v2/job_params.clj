(ns mescal.agave-de-v2.job-params
  (:use [mescal.agave-de-v2.params :only [get-param-type]])
  (:require [mescal.util :as util]))

(defn- format-param-value
  [get-val get-default get-type get-format get-info-type param]
  (let [default   (str (get-default))
        param-val (str (get-val))]
    {:data_format      (get-format)
     :full_param_id    (:id param)
     :info_type        (get-info-type)
     :is_default_value (= param-val default)
     :is_visible       (util/get-boolean (get-in param [:value :visible]) false)
     :param_id         (:id param)
     :param_name       (get-in param [:details :label] "")
     :param_type       (get-type param)
     :param_value      {:value param-val}}))

(defn- get-param-value
  [param-values param]
  (param-values (keyword (:id param)) ""))

(defn- get-default-param-value
  [param]
  (:defaultValue param ""))

(defn- format-input-param-value
  [agave param-values param]
  (format-param-value #(.irodsFilePath agave (get-param-value param-values param))
                      #(.irodsFilePath agave (get-default-param-value param))
                      (constantly "FileInput")
                      (constantly "Unspecified")
                      (constantly "File")
                      param))

(defn- format-opt-param-value
  [param-values param]
  (format-param-value #(get-param-value param-values param)
                      #(get-default-param-value param)
                      get-param-type
                      (constantly "")
                      (constantly "")
                      param))

(defn format-params
  [agave job app-id app]
  (let [format-input (partial format-input-param-value agave (:inputs job))
        format-opt   (partial format-opt-param-value (:parameters job))]
    {:analysis_id app-id
     :parameters  (concat (mapv format-input (:inputs app))
                          (mapv format-opt   (:parameters app)))}))

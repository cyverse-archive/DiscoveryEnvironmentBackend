(ns mescal.agave-de-v2.job-params
  (:require [mescal.agave-de-v2.params :as mp]
            [mescal.util :as util]))

(defn- format-param-value
  [get-val get-default get-type get-format get-info-type param]
  (let [default   (get-default)
        param-val (get-val)]
    {:data_format      (get-format)
     :full_param_id    (:id param)
     :info_type        (get-info-type)
     :is_default_value (= param-val default)
     :is_visible       (util/get-boolean (get-in param [:value :visible]) false)
     :param_id         (:id param)
     :param_name       (get-in param [:details :label] "")
     :param_type       (get-type param)
     :param_value      {:value param-val}}))

(defn- get-default-param-value
  [param]
  (let [value-obj   (:value param)
        enum-values (util/get-enum-values value-obj)
        default     (first (:default value-obj))]
    (if (mp/enum-param? param)
      (mp/format-enum-element default (mp/find-enum-element default enum-values))
      default)))

(defn- get-param-value
  [param-values param]
  (when-let [param-value (param-values (keyword (:id param)) "")]
    (if (mp/enum-param? param)
      (let [{value-obj :value} param
            enum-values        (util/get-enum-values value-obj)
            default            (first (:default value-obj))]
        (mp/format-enum-element default (mp/find-enum-element param-value enum-values)))
      param-value)))

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
                      mp/get-param-type
                      (constantly "")
                      (constantly "")
                      param))

(defn format-params
  [agave job app-id app]
  (let [format-input (partial format-input-param-value agave (:inputs job))
        format-opt   (partial format-opt-param-value (:parameters job))]
    {:app_id     app-id
     :parameters (concat (mapv format-input (:inputs app))
                         (mapv format-opt   (:parameters app)))}))

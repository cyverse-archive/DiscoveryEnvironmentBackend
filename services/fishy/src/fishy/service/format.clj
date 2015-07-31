(ns fishy.service.format
  (:use [medley.core :only [remove-vals]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as ce]))

(defn- not-found
  [response]
  (throw+ {:error_code          ce/ERR_NOT_FOUND
           :grouper_result_code (:resultCode response)
           :id                  (:id response)}))

(defn format-group
  [group]
  (->> {:description       (:description group)
        :display_extension (:displayExtension group)
        :display_name      (:displayName group)
        :extension         (:extension group)
        :id_index          (:idIndex group)
        :name              (:name group)
        :type              (:typeOfGroup group)
        :id                (:uuid group)}
       (remove-vals nil?)))

(defn format-folder
  [folder]
  (->> {:description       (:description folder)
        :display_extension (:displayExtension folder)
        :display_name      (:displayName folder)
        :extension         (:extension folder)
        :id_index          (:idIndex folder)
        :name              (:name folder)
        :id                (:uuid folder)}
       (remove-vals nil?)))

(defn format-subject
  [subject]
  (condp = (:resultCode subject)
    "SUBJECT_NOT_FOUND" (not-found subject)
    (->> {:attribute_values  (:attributeValues subject)
          :id                (:id subject)
          :name              (:name subject)
          :source_id         (:sourceId subject)}
         (remove-vals nil?))))

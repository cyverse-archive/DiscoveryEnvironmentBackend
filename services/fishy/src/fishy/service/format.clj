(ns fishy.service.format
  (:use [medley.core :only [remove-vals]]))

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
  (->> {:attribute_values  (:attributeValues subject)
        :id                (:id subject)
        :name              (:name subject)
        :source_id         (:sourceId subject)}
       (remove-vals nil?)))

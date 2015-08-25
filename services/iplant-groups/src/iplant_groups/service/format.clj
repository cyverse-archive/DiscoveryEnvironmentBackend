(ns iplant_groups.service.format
  (:use [medley.core :only [remove-vals]]
        [slingshot.slingshot :only [throw+]])
  (:require [clj-time.format :as tf]
            [clojure.string :as string]
            [clojure-commons.error-codes :as ce]))

(def ^:private timestamp-formatter (tf/formatter "yyyy/MM/dd HH:mm:ss.SSSSSS"))

(defn timestamp-to-millis
  [timestamp]
  (when-not (nil? timestamp)
    (.getMillis (tf/parse timestamp-formatter timestamp))))

(defn string-to-boolean
  [s]
  (when-not (nil? s)
    (condp = (string/lower-case s)
      "true"  true
      "t"     true
      "yes"   true
      "y"     true
      "false" false
      "f"     false
      "no"    false
      "n"     false
      (throw+ {:error_code    ce/ERR_ILLEGAL_ARGUMENT
               :boolean_value s}))))

(defn- not-found
  [response]
  (throw+ {:error_code          ce/ERR_NOT_FOUND
           :grouper_result_code (:resultCode response)
           :id                  (:id response)}))

(defn format-group
  [group]
  (when-not (nil? group)
    (->> {:description       (:description group)
          :display_extension (:displayExtension group)
          :display_name      (:displayName group)
          :extension         (:extension group)
          :id_index          (:idIndex group)
          :name              (:name group)
          :type              (:typeOfGroup group)
          :id                (:uuid group)}
         (remove-vals nil?))))

(defn format-group-detail
  [detail]
  (->> {:attribute_names     (:attributeNames detail)
        :attribute_values    (:attributeValues detail)
        :composite_type      (:compositeType detail)
        :created_at          (timestamp-to-millis (:createTime detail))
        :created_by          (:createSubjectId detail)
        :has_composite       (string-to-boolean (:hasComposite detail))
        :is_composite_factor (string-to-boolean (:isCompositeFactor detail))
        :left_group          (format-group (:leftGroup detail))
        :modified_at         (timestamp-to-millis (:modifyTime detail))
        :modified_by         (:modifySubjectId detail)
        :right_group         (format-group (:rightGroup detail))
        :type_names          (:typeNames detail)}
       (remove-vals nil?)))

(defn format-group-with-detail
  [group]
  (->> (assoc (format-group group)
         :detail (format-group-detail (:detail group)))
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
  [attribute-names subject]
  (condp = (:resultCode subject)
    "SUBJECT_NOT_FOUND" (not-found subject)
    (let [known-keys #{"mail" "givenName" "sn" "o"}
          known-mappings (keep-indexed #(if (contains? known-keys %2) [%2 %1]) attribute-names)
          known-key-indexes (into {} known-mappings)]
      (->> {:attribute_values  (keep-indexed #(if (not (contains? (set (map second known-mappings)) %1)) %2)
                                             (:attributeValues subject))
            :id                (:id subject)
            :name              (:name subject)
            :first_name        (nth (:attributeValues subject) (get known-key-indexes "givenName"))
            :last_name         (nth (:attributeValues subject) (get known-key-indexes "sn"))
            :email             (nth (:attributeValues subject) (get known-key-indexes "mail"))
            :institution       (nth (:attributeValues subject) (get known-key-indexes "o"))
            :source_id         (:sourceId subject)}
           (remove-vals nil?)
           (remove-vals empty?)))))

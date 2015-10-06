(ns facepalm.c140-2012082201
  (:use [korma.core]
        [kameleon.core]
        [facepalm.error-codes :only [conversion-validation-error]])
  (:require [clojure.string :as string])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.4.0:20120822.01")

;; An entity that can be used to find duplicated validator IDs.
(defentity validator_count
  (table (subselect :validator
                    (fields :id [(sqlfn count :id) :count])
                    (group :id))
         :validator_count))

;; An entity that can be used to find duplicated property group IDs.
(defentity property_group_count
  (table (subselect :property_group
                    (fields :id [(sqlfn count :id) :count])
                    (group :id))
         :property_group_count))

(defn- uuid []
  "Generates a random UUID."
  (-> (UUID/randomUUID)
      str
      string/upper-case))

(defn- find-duplicated-validator-ids
  "Finds the list of duplicated validator identifiers in the database."
  []
  (map :id (select validator_count (where {:count [> 1]}))))

(defn- update-validator-id
  "Updates the identifier of a validator."
  [{:keys [hid]}]
  (update :validator
          (set-fields {:id (uuid)})
          (where {:hid hid})))

(defn- fix-duplicated-validator-ids
  "Removes duplicate validator identifiers from the database."
  ([]
     (println "\t* eliminating duplicate validator IDs")
     (dorun (map fix-duplicated-validator-ids (find-duplicated-validator-ids))))
  ([id]
     (dorun (map update-validator-id
                 (drop 1 (select :validator (where {:id id})))))))

(defn- find-duplicated-property-group-ids
  "Finds the list of duplicated property group identifiers in the database."
  []
  (map :id (select property_group_count (where {:count [> 1]}))))

(defn- update-property-group-id
  "Updates the identifier of a property group."
  [{:keys [hid]}]
  (update :property_group
          (set-fields {:id (uuid)})
          (where {:hid hid})))

(defn- fix-duplicated-property-group-ids
  "Removes duplicated property group identifiers from the database."
  ([]
     (println "\t* eliminating duplicate property group IDs")
     (dorun (map fix-duplicated-property-group-ids
                 (find-duplicated-property-group-ids))))
  ([id]
     (dorun (map update-property-group-id
                 (drop 1 (select :property_group (where {:id id})))))))

(defn- load-props-for-template
  "Loads the list of properties associated with a template."
  [template-id]
  (select :property
          (join :property_group_property
                (= :property.hid :property_group_property.property_id))
          (join :property_group
                (= :property_group_property.property_group_id
                   :property_group.hid))
          (join :template_property_group
                (= :property_group.hid
                   :template_property_group.property_group_id))
          (join :template
                (= :template_property_group.template_id :template.hid))
          (where {:template.id template-id})))

(defn- count-props
  "Counts the number of properties with the given identifier."
  [id]
  (:count (first (select :property
                         (aggregate (count :*) :count)
                         (where {:id id})))))

(defn- update-property-id
  "Updates the identifier for a single property.  If there's an associated
   data object then the data object's property should have the same ID."
  [{:keys [hid]}]
  (let [new-id (uuid)]
    (update :property
            (set-fields {:id new-id})
            (where {:hid hid}))
    new-id))

(defn- update-property-ids-for
  "Updates the property identifiers in a template."
  [template-id]
  (letfn [(uuid? [id]
            (or (re-matches #"[0-9A-F]{8}-(?:[0-9A-F]{4}-){3}[0-9A-F]{12}" id)
                (re-matches #"p[0-9a-f]{32}" id)))
          (should-replace-id? [{:keys [id]}]
            (cond (> (count-props id) 1) true
                  (uuid? id)             false
                  :else                  true))]
    (->> (load-props-for-template template-id)
        (filter should-replace-id?)
        (map #(vector (:id %) (update-property-id %)))
        (into {}))))

(defn- load-transformations-for-app
  "Loads the transformations associated with an app from the database."
  [app-id]
  (select :transformations
          (join :transformation_steps
                (= :transformations.id :transformation_steps.transformation_id))
          (join :transformation_task_steps
                (= :transformation_steps.id
                   :transformation_task_steps.transformation_step_id))
          (join :transformation_activity
                (= :transformation_activity.hid
                   :transformation_task_steps.transformation_task_id))
          (where {:transformation_activity.id app-id})))

(defn- update-transformation-value-property-id
  "Updates the property ID in a transformation value table entry if necessary."
  [id-map {:keys [id property]}]
  (when (contains? id-map property)
    (update :transformation_values
            (set-fields {:property (id-map property)})
            (where {:id id}))))

(defn- fix-property-ids-in-transformation
  "Updates the property IDs in a transformation to correspond to the updated
   property IDs in the associated template."
  [prop-maps {tx-id :id template-id :template_id}]
  (let [id-map (prop-maps template-id)]
    (dorun (map #(update-transformation-value-property-id id-map %)
                (select :transformation_values
                       (where {:transformation_id tx-id}))))))

(defn- update-step-configs-for-app
  "Updates the step configurations (that is, the property values specified in
   the transformations) for an app."
  [prop-maps app-id]
  (dorun (map #(fix-property-ids-in-transformation prop-maps %)
              (load-transformations-for-app app-id))))

(defn- load-io-mappings-for-app
  "Loads the input/output mappings for an app."
  [app-id]
  (select :input_output_mapping
          (join :transformation_activity_mappings
                (= :input_output_mapping.hid
                   :transformation_activity_mappings.mapping_id))
          (join :transformation_activity
                (= :transformation_activity_mappings.transformation_activity_id
                   :transformation_activity.hid))
          (where {:transformation_activity.id app-id})))

(defn- template-id-for-step
  "Determines the template ID for the transformation step with the given ID."
  [step-id]
  (:template_id
   (first
    (select :transformations
            (fields :template_id)
            (join :transformation_steps
                  (= :transformations.id
                     :transformation_steps.transformation_id))
            (where {:transformation_steps.id step-id})))))

(defn- fix-dataobject-mapping
  "Fixes a dataobject mapping for a multistep app."
  [source-id-map target-id-map {:keys [mapping_id input output]}]
  (when (or (contains? source-id-map output) (contains? target-id-map input))
    (update :dataobject_mapping
            (set-fields {:input  (or (target-id-map input) input)
                         :output (or (source-id-map output) output)})
            (where {:mapping_id mapping_id
                    :input      input
                    :output     output}))))

(defn- fix-io-mapping
  "Fixes an input/output mapping for an app."
  [prop-maps {:keys [hid source target]}]
  (let [source-id-map (prop-maps (template-id-for-step source))
        target-id-map (prop-maps (template-id-for-step target))]
    (dorun (map #(fix-dataobject-mapping source-id-map target-id-map %)
                (select :dataobject_mapping (where {:mapping_id hid}))))))

(defn- update-dataobject-mappings-for-app
  "Updates the input/output mappings for a multistep app."
  [prop-maps app-id]
  (dorun (map #(fix-io-mapping prop-maps %)
              (load-io-mappings-for-app app-id))))

(defn- update-app-property-references
  "Updates references to properties within an app."
  [prop-maps app-id]
  (update-step-configs-for-app prop-maps app-id)
  (update-dataobject-mappings-for-app prop-maps app-id))

(defn- fix-duplicated-property-ids
  "Removes duplicated property identifiers from the database."
  []
  (println "\t* eliminating duplicate property IDs; this could take a while")
  (let [template-ids (map :id (select :template (fields :id)))
        prop-maps    (into {} (map #(vector % (update-property-ids-for %))
                                   template-ids))
        app-ids      (map :id (select :transformation_activity (fields :id)))]
    (dorun (map #(update-app-property-references prop-maps %) app-ids))))

(defn- fix-dataobject-ids
  "Fixes any data object IDs that were broken by the conversion."
  []
  (println "\t* fixing any data object IDs that were broken by the conversion")
  (exec-raw
   "UPDATE dataobjects
    SET id = p.id
    FROM property p
    WHERE p.dataobject_id = dataobjects.hid"))

(defn- prop-ids-for-template
  "Obtains the set of property identifiers associated with a template."
  [template-id]
  (set (map :id (load-props-for-template template-id))))

(defn- validate-transformation-value
  "Verifies that a single transformation value appears to be correct after the
   conversion."
  [{property-id :property template-id :template_id}]
  (let [prop-ids (prop-ids-for-template template-id)]
    (when-not (contains? prop-ids property-id)
      (conversion-validation-error
       version {:error-code  :invalid-property-id-in-transformation-value
                :property-id property-id
                :template-id template-id}))))

(defn- validate-step-configs
  "Verifies that the step configurations appear to be correct after the
   conversion."
  []
  (println "\t* validating the updated transformation configurations")
  (dorun (map validate-transformation-value
              (select :transformation_values
                      (fields :transformation_values.property
                              :transformations.template_id)
                      (join :transformations
                            (= :transformation_values.transformation_id
                               :transformations.id))))))

(defn- validate-dataobject-mapping
  "Validates a single data object mapping."
  [source-props target-props {:keys [mapping_id input output]}]
  (if-not (and (contains? target-props input) (contains? source-props output))
    (conversion-validation-error
     version {:error-code :invalid-property-id-in-io-mapping
              :mapping-id mapping_id
              :input-id   input
              :output-id  output})))

(defn- mapping-belongs-to-deleted-app
  "Determines whether or not an input/output mapping belongs to a deleted app."
  [mapping-id]
  ((comp :deleted first)
   (select
    :transformation_activity_mappings
    (fields :transformation_activity.deleted)
    (join :transformation_activity
          (= :transformation_activity.hid
             :transformation_activity_mappings.transformation_activity_id))
    (where {:transformation_activity_mappings.mapping_id mapping-id}))))

(defn- validate-io-mapping
  "Verifies a single input/output mapping after the conversion.  The mappings
   for some deleted apps were already broken, so this validation is only
   performed for apps that are not marked as deleted."
  [{:keys [hid source target]}]
  (when-not (mapping-belongs-to-deleted-app hid)
    (let [source-props (prop-ids-for-template (template-id-for-step source))
          target-props (prop-ids-for-template (template-id-for-step target))]
      (dorun (map #(validate-dataobject-mapping source-props target-props %)
                  (select :dataobject_mapping (where {:mapping_id hid})))))))

(defn- validate-io-mappings
  "Verifies that the input/output mappings appear to be correct after the
   conversion."
  []
  (println "\t* validating the updated input/output mappings")
  (dorun (map validate-io-mapping (select :input_output_mapping))))

(defn- count-invalid-dataobject-ids
  "Counts the number of data objects for which the ID is not the same as the ID
   of the parent property."
  []
  (-> (select :property
              (aggregate (count :*) :count)
              (join :data_object)
              (where {:property.id [not= :data_object.id]}))
      first
      :count))

(defn- validate-dataobject-ids
  "Verifies that the ID of each data object is the same as the ID of its parent
   property."
  []
  (println "\t* verifying data object IDs")
  (when-not (zero? (count-invalid-dataobject-ids))
    (conversion-validation-error
     version {:error-code :invalid-dataobject-ids-found})))

(defn convert
 "Performs the conversions for database version 1.4.0:20120822.01."
 []
 (println "Performing conversion for" version)
 (fix-duplicated-validator-ids)
 (fix-duplicated-property-group-ids)
 (fix-duplicated-property-ids)
 (fix-dataobject-ids)
 (validate-step-configs)
 (validate-io-mappings)
 (validate-dataobject-ids))

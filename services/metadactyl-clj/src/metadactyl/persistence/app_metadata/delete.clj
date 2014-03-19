(ns metadactyl.persistence.app-metadata.delete
  "Functions used to remove apps from the database."
  (:use [kameleon.entities]
        [korma.core]))

(defn- remove-app-from-groups
  "Removes an app from all app groups."
  [app-hid]
  (delete :template_group_template
          (where {:template_id app-hid})))

(defn- remove-app-ratings
  "Removes any ratings that were associated with an app."
  [app-hid]
  (delete :ratings
          (where {:transformation_activity_id app-hid})))

(defn- remove-app-suggested-groups
  "Removes any suggested groups that were associated with an app."
  [app-hid]
  (delete :suggested_groups
          (where {:transformation_activity_id app-hid})))

(defn- input-output-mapping-ids-for-app
  "Loads the list of input/output mappings for a multi-step app."
  [app-hid]
  (->> (select :transformation_activity_mappings
               (fields :mapping_id)
               (where {:transformation_activity_id app-hid}))
       (mapv :mapping_id)))

(defn- remove-app-mappings
  "Removes input/output mappings from a multi-step app."
  [app-hid]
  (let [mapping-ids (input-output-mapping-ids-for-app app-hid)]
    (delete :dataobject_mapping
            (where {:mapping_id [in mapping-ids]}))
    (delete :transformation_activity_mappings
            (where {:transformation_activity_id app-hid}))
    (delete :input_output_mapping
            (where {:hid [in mapping-ids]}))))

(defn- remove-app-references
  "Removes any references associated with an app."
  [app-hid]
  (delete :transformation_activity_references
          (where {:transformation_activity_id app-hid})))

(defn- transformations-for-app
  "Loads the list of transformations associated with an app."
  [app-hid]
  (select [:transformation_task_steps :tts]
          (join [:transformation_steps :ts]
                {:tts.transformation_step_id :ts.id})
          (join [:transformations :tx]
                {:ts.transformation_id :tx.id})
          (where {:tts.transformation_task_id app-hid})))

(defn- property-groups-in-template
  "Retrieves the list of property groups associated with a template."
  [template-hid]
  (select [:template_property_group :tpg]
          (join [:property_group :pg]
                {:tpg.property_group_id :pg.hid})
          (fields :pg.hid :pg.id :pg.name :pg.description :pg.label)
          (where {:template_id template-hid})))

(defn- properties-in-group
  "Retrieves the list of properties associated with a property group."
  [group-hid]
  (select [:property_group_property :pgp]
          (join [:property :p]
                {:pgp.property_id :p.hid})
          (fields :p.hid :p.id :p.name :p.description :p.label :p.validator :p.dataobject_id)
          (where {:property_group_id group-hid})))

(defn- rules-in-validator
  "Retrieves the list of rules in a validator."
  [validator-hid]
  (select [:validator_rule :vr]
          (join [:rule :r] {:vr.rule_id :r.hid})
          (fields :r.hid :r.id :r.name :r.description :r.label)
          (where {:vr.vaidator_id validator-hid})))

(defn- delete-rule
  "Removes a rule from the database."
  [rule]
  (delete :rule_argument (where {:rule_id (:hid rule)}))
  (delete :rule (where {:hid (:hid rule)})))

(defn- delete-validator
  "Removes a property validator from the database."
  [validator-hid]
  (let [rules (mapv delete-rule (rules-in-validator validator-hid))]
    (delete :validator_rule (where {:validator_id validator-hid}))
    (dorun (map delete-rule rules))
    (delete :validator (where {:hid validator-hid}))))

(defn- delete-data-object
  "Removes a data object from the database."
  [dataobject-hid]
  (delete :template_input (where {:input_id dataobject-hid}))
  (delete :template_output (where {:output_id dataobject-hid}))
  (delete :dataobjects (where {:hid dataobject-hid})))

(defn- delete-property
  "Deletes a property from the database."
  [prop]
  (delete :property (where {:hid (:hid prop)}))
  (delete-validator (:validator prop))
  (delete-data-object (:dataobject_id prop)))

(defn- delete-property-group
  "Deletes a property group from the database."
  [group-hid]
  (let [props (properties-in-group group-hid)]
    (delete :property_group_property (where {:property_group_id group-hid}))
    (dorun (map delete-property (properties-in-group group-hid)))
    (delete :property_group (where {:hid group-hid}))))

(defn- template-orphaned?
  "Determines whether or not a template is orphaned."
  [template-id]
  ((comp zero? :count first)
   (select :transformations
           (aggregate (count :*) :count)
           (where {:template_id template-id}))))

(defn- delete-orphaned-template
  "Deletes a template if it's orphaned (that is, if it's not used in any app)."
  [template-id]
  (when (template-orphaned? template-id)
    (when-let [template (first (select :template (where {:id template-id})))]
      (let [groups (property-groups-in-template (:hid template))]
        (delete :template_property_group (where {:template_id (:hid template)}))
        (dorun (map (comp delete-property-group :hid) groups))
        (delete :template_input (where {:template_id (:hid template)}))
        (delete :template_output (where {:template_id (:hid template)}))
        (delete :template (where {:hid (:hid template)}))))))

(defn- remove-app-steps
  "Removes any transformation steps associated with an app."
  [app-hid]
  (let [txs          (transformations-for-app app-hid)
        tx-ids       (mapv :id txs)
        template-ids (mapv :template_id txs)]
    (delete :transformation_values
            (where {:transformation_id [in tx-ids]}))
    (delete :transformation_task_steps
            (where {:transformation_task_id app-hid}))
    (delete :transformation_steps
            (where {:transformation_id [in tx-ids]}))
    (delete :transformations
            (where {:id [in tx-ids]}))
    (dorun (map delete-orphaned-template template-ids))))

(defn- remove-app
  "Removes an app from the database."
  [app-hid]
  (delete :transformation_activity
          (where {:hid app-hid})))

(defn permanently-delete-app
  "Permanently removes an app from the database."
  [app-hid]
  (remove-app-from-groups app-hid)
  (remove-app-ratings app-hid)
  (remove-app-suggested-groups app-hid)
  (remove-app-mappings app-hid)
  (remove-app-references app-hid)
  (remove-app-steps app-hid)
  (remove-app app-hid))

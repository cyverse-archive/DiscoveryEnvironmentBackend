(ns metadactyl.app-validation
  (:use [slingshot.slingshot :only [try+ throw+]]
        [korma.core]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.queries :only [property-types-for-tool-type]])
  (:require [clojure.string :as string])
  (:import [org.iplantc.persistence.dto.components DeployedComponent]
           [org.iplantc.workflow MissingDeployedComponentException
            UnknownDeployedComponentException]
           [org.iplantc.workflow.integration.validation
            UnsupportedPropertyTypeException]))

(defn- get-tool-type-from-database
  "Gets the tool type for the deployed component with the given identifier from
   the database."
  [component-id]
  (first (select deployed_components
                 (fields :tool_types.id :tool_types.name)
                 (join tool_types)
                 (where {:id component-id}))))

(defn- get-deployed-component-from-database
  "Gets the deployed component for the deployed component with the given identifer
   from the database."
  [component-id]
  (first (select deployed_components
                 (where {:id component-id}))))

(defn- get-tool-type-from-registry
  "Gets the tool type for the deployed component with the given identifier from
   the given registry."
  [registry component-id]
  (when-not (nil? registry)
    (let [components (.getRegisteredObjects registry DeployedComponent)
          component  (first (filter #(= component-id (.getId %)) components))
          tool-type  (when-not (nil? component) (.getToolType component))]
      (when-not (nil? tool-type)
        {:id   (.getId tool-type)
         :name (.getName tool-type)}))))

(defn- get-tool-type
  "Gets the tool type name for the deployed component with the given identifier."
  [registry component-id]
  (or (get-tool-type-from-registry registry component-id)
      (get-tool-type-from-database component-id)))

(defn- get-valid-ptype-names
  "Gets the valid property type names for a given tool type."
  [{tool-type-id :id}]
  (map :name (property-types-for-tool-type tool-type-id)))

(defn validate-template-property-types
  "Validates the property types in a template that is being imported."
  [template registry]
  (when-let [tool-type (get-tool-type registry (.getComponent template))]
    (let [valid-ptypes (into #{} (get-valid-ptype-names tool-type))
          properties   (mapcat #(.getProperties %) (.getPropertyGroups template))]
      (dorun (map #(throw (UnsupportedPropertyTypeException. % (:name tool-type)))
                  (filter #(nil? (valid-ptypes %))
                          (map #(.getPropertyTypeName %) properties)))))))

(defn validate-template-deployed-component
  "Validates a deployed component that is associated with a template."
  [template]
  (let [component-id (.getComponent template)]
   (when (string/blank? component-id)
     (throw (MissingDeployedComponentException. (.getId template))))
   (when (nil? (get-deployed-component-from-database component-id))
     (throw (UnknownDeployedComponentException. component-id)))))

(defn- template-ids-for-app
  "Get the list of template IDs associated with an app."
  [app-id]
  (map :template_id
       (select [:analysis_listing :a]
               (fields :tx.template_id)
               (join [:transformation_task_steps :tts]
                     {:a.hid :tts.transformation_task_id})
               (join [:transformation_steps :ts]
                     {:tts.transformation_step_id :ts.id})
               (join [:transformations :tx]
                     {:ts.transformation_id :tx.id})
               (where {:a.id app-id}))))

(defn- private-apps-for
  "Finds private single-step apps for a list of template IDs."
  [template-ids]
  (select [:analysis_listing :a]
          (fields :a.id :a.name)
          (join [:transformation_task_steps :tts]
                {:a.hid :tts.transformation_task_id})
          (join [:transformation_steps :ts]
                {:tts.transformation_step_id :ts.id})
          (join [:transformations :tx]
                {:ts.transformation_id :tx.id})
          (where {:tx.template_id [in template-ids]
                  :a.step_count   1
                  :a.is_public    false})))

(defn app-publishable?
  "Determines whether or not an app can be published. An app is publishable if none of the
   templates in the app are associated with any single-step apps that are not public. Returns
   a flag indicating whether or not the app is publishable along with the reason the app isn't
   publishable if it's not."
  [app-id]
  (if (string/blank? app-id)
    [false "no app ID provided"]
    (let [template-ids (template-ids-for-app app-id)
          private-apps (private-apps-for template-ids)]
      (cond (zero? (count template-ids)) [false "no app ID provided"]
            (= 1 (count template-ids))   [true]
            (pos? (count private-apps))  [false "contains private apps" private-apps]
            :else                        [true]))))

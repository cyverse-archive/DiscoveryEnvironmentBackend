(ns donkey.persistence.apps
  "Functions for storing and retrieving information about apps that can be executed
   within the DE, excluding external apps such as Agave apps."
  (:use [kameleon.entities :only [app_listing]]
        [korma.core]
        [korma.db :only [with-db]])
  (:require [donkey.util.db :as db]))

(defn load-app-details
  [app-ids]
  (with-db db/de
    (select app_listing
            (where {:id [in app-ids]}))))

(defn get-app-properties
  [app-id]
  (with-db db/de
    (select [:transformation_activity :app]
            (fields :p.id
                    :p.name
                    :p.description
                    :p.label
                    [:p.defalut_value :default_value]
                    :p.is_visible
                    :p.ordering
                    :p.omit_if_blank
                    [:pt.name :type]
                    :d.is_implicit
                    [:info_type.name :info_type]
                    [:df.name :data_format]
                    [:ts.name :step_name]
                    [:tx.external_app_id :external_app_id])
            (join [:transformation_task_steps :tts]
                  {:app.hid :tts.transformation_task_id})
            (join [:transformation_steps :ts]
                  {:tts.transformation_step_id :ts.id})
            (join [:transformations :tx]
                  {:ts.transformation_id :tx.id})
            (join [:template :t]
                  {:tx.template_id :t.id})
            (join [:template_property_group :tpg]
                  {:tpg.template_id :t.hid})
            (join [:property_group :pg]
                  {:pg.hid :tpg.property_group_id})
            (join [:property_group_property :pgp]
                  {:pgp.property_group_id :pg.hid})
            (join [:property :p]
                  {:p.hid :pgp.property_id})
            (join [:property_type :pt]
                  {:p.property_type :pt.hid})
            (join [:dataobjects :d]
                  {:d.hid :p.dataobject_id})
            (join [:data_formats :df]
                  {:df.id :d.data_format})
            (join :info_type
                  {:info_type.hid :d.info_type})
            (where {:app.id app-id}))))

(defn- default-output-name-base-query
  []
  (-> (select* [:template :t])
      (join [:template_property_group :tpg] {:t.hid :tpg.template_id})
      (join [:property_group :pg] {:tpg.property_group_id :pg.hid})
      (join [:property_group_property :pgp] {:pg.hid :pgp.property_group_id})
      (join [:property :p] {:pgp.property_id :p.hid})
      (join [:dataobjects :d] {:p.dataobject_id :d.hid})
      (join [:property_type :pt] {:p.property_type :pt.hid})
      (fields [:d.name :default_value])
      (where {:pt.name "Output"})))

(defn get-default-output-name
  [template-id property-id]
  (with-db db/de
    (some->> (select (default-output-name-base-query)
                     (where {:t.id template-id
                             :p.id property-id}))
             (first)
             (:default_value))))

(defn load-app-steps
  [app-id]
  (with-db db/de
    (select [:transformation_activity :a]
            (join [:transformation_task_steps :tts] {:a.hid :tts.transformation_task_id})
            (join [:transformation_steps :ts] {:tts.transformation_step_id :ts.id})
            (join [:transformations :tx] {:ts.transformation_id :tx.id})
            (fields [:ts.id              :step_id]
                    [:ts.name            :step_name]
                    [:tx.template_id     :template_id]
                    [:tx.external_app_id :external_app_id])
            (where {:a.id app-id}))))

(defn load-app-info
  [app-id]
  (first
   (with-db db/de
     (select [:transformation_activity :a]
             (where {:id app-id})))))

(defn- mapping-base-query
  []
  (-> (select* [:input_output_mapping :iom])
      (join [:transformation_steps :source] {:iom.source :source.id})
      (join [:transformation_steps :target] {:iom.target :target.id})
      (join [:dataobject_mapping :dom] {:iom.hid :dom.mapping_id})
      (fields [:dom.input    :input_id]
              [:target.id    :target_id]
              [:target.name  :target_name]
              [:dom.output   :output_id]
              [:source.id    :source_id]
              [:source.name  :source_name])))

(defn load-target-step-mappings
  [step-id]
  (with-db db/de
    (select (mapping-base-query)
            (where {:iom.target step-id}))))

(defn load-app-mappings
  [app-id]
  (with-db db/de
    (select (mapping-base-query)
            (join [:transformation_task_steps :tts] {:iom.target :tts.transformation_step_id})
            (join [:transformation_activity :a] {:tts.transformation_task_id :a.hid})
            (where {:a.id app-id}))))

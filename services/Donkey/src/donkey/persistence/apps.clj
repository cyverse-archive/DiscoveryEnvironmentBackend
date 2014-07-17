(ns donkey.persistence.apps
  "Functions for storing and retrieving information about apps that can be executed
   within the DE, excluding external apps such as Agave apps."
  (:use [kameleon.entities :only [analysis_listing]]
        [korma.core]
        [korma.db :only [with-db]])
  (:require [donkey.util.db :as db]))

(defn load-app-details
  [app-ids]
  (with-db db/de
    (select analysis_listing
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

(defn load-target-step-mappings
  [step-id]
  (with-db db/de
    (select [:input_output_mapping :iom]
            (join [:transformation_steps :source] {:iom.source :source.id})
            (join [:transformation_steps :target] {:iom.target :target.id})
            (join [:dataobject_mapping :dom] {:iom.hid :dom.mapping_id})
            (fields [:dom.input    :input_id]
                    [:target.name  :target_name]
                    [:dom.output   :output_id]
                    [:source.name  :source_name])
            (where {:iom.target step-id}))))

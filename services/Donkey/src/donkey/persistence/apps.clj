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

(defn- default-value-subselect
  []
  (subselect [:parameter_values :pv]
             (fields [:pv.value :default_value])
             (where {:pv.parameter_id :p.id
                     :pv.is_default   true})))

(defn get-app-parameters
  [app-id]
  (with-db db/de
    (select [:apps :app]
            (fields :p.id
                    :p.name
                    :p.description
                    :p.label
                    [(default-value-subselect) :default_value]
                    :p.is_visible
                    :p.ordering
                    :p.omit_if_blank
                    [:pt.name :type]
                    :fp.is_implicit
                    [:info_type.name :info_type]
                    [:df.name :data_format]
                    [:s.id :step_id]
                    [:t.external_app_id :external_app_id])
            (join [:app_steps :s]
                  {:app.id :s.app_id})
            (join [:tasks :t]
                  {:s.task_id :t.id})
            (join [:parameter_groups :pg]
                  {:pg.task_id :t.id})
            (join [:parameters :p]
                  {:p.parameter_group_id :pg.id})
            (join [:parameter_types :pt]
                  {:p.parameter_type :pt.id})
            (join [:file_parameters :fp]
                  {:fp.parameter_id :p.id})
            (join [:data_formats :df]
                  {:df.id :fp.data_format})
            (join :info_type
                  {:info_type.id :fp.info_type})
            (where {:app.id app-id}))))

(defn get-default-output-name
  [task-id parameter-id]
  (with-db db/de
    (some->> (-> (select* [:tasks :t])
                 (join [:parameter_groups :pg] {:t.id :pg.task_id})
                 (join [:parameters :p] {:pg.id :p.parameter_group_id})
                 (join [:parameter_values :pv] {:p.id :pv.parameter_id})
                 (fields [:pv.value :default_value])
                 (where {:pv.is_default true
                         :t.id          task-id
                         :p.id          parameter-id})
                 (select))
             (first)
             (:default_value))))

(defn load-app-steps
  [app-id]
  (with-db db/de
    (select [:apps :a]
            (join [:app_steps :s] {:a.id :s.app_id})
            (join [:tasks :t] {:s.task_id :t.id})
            (fields [:s.id              :step_id]
                    [:t.tool_id         :tool_id]
                    [:t.external_app_id :external_app_id])
            (where {:a.id app-id}))))

(defn load-app-info
  [app-id]
  (first
   (with-db db/de
     (select [:apps :a] (where {:id app-id})))))

(defn- mapping-base-query
  []
  (-> (select* [:workflow_io_maps :wim])
      (join [:input_output_mapping :iom] {:wim.id :iom.mapping_id})
      (fields [:wim.source_step     :source_id]
              [:wim.target_step     :target_id]
              [:iom.input           :input_id]
              [:iom.external_input  :external_input_id]
              [:iom.output          :output_id]
              [:iom.external_output :external_output_id])))

(defn load-target-step-mappings
  [step-id]
  (with-db db/de
    (select (mapping-base-query)
            (where {:wim.target_step step-id}))))

(defn load-app-mappings
  [app-id]
  (with-db db/de
    (select (mapping-base-query)
            (where {:wim.app_id app-id}))))

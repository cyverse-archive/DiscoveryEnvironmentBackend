(ns facepalm.c140-2012072601
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.4.0:20120726.01")

(defn- drop-component-listing-views
  "Drops any views that reference the type column in the deployed_components
   table."
  []
  (exec-raw "DROP VIEW analysis_listing")
  (exec-raw "DROP VIEW deployed_component_listing")
  (exec-raw "DROP VIEW analysis_job_types"))

(defn- add-tool-types-table
  "Adds the tool_types table and its ID sequence."
  []
  (println "\t* adding the tool_types table")
  (exec-raw
   "CREATE SEQUENCE tool_types_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MAXVALUE
        NO MINVALUE
        CACHE 1")
  (exec-raw
   "CREATE TABLE tool_types (
        id bigint DEFAULT nextval('tool_types_id_seq'::regclass) NOT NULL,
        name varchar(50) UNIQUE NOT NULL,
        label varchar(128) NOT NULL,
        description varchar(256),
        PRIMARY KEY(id))"))

(defn- populate-tool-types-table
  "Populates the tool_types table."
  []
  (println "\t* populating the tool_types table")
  (insert :tool_types
          (values {:name        "executable"
                   :label       "UA"
                   :description "Run at the University of Arizona"})
          (values {:name        "fAPI"
                   :label       "TACC"
                   :description "Run at the Texas Advanced Computing Center"})))

(defn- add-tool-type-id-to-deployed-components
  "Adds the foreign key, tool_type_id, to the deployed_components table"
  []
  (println "\t* referencing tool_types from deployed_components")
  (exec-raw
   "ALTER TABLE deployed_components
        ADD COLUMN tool_type_id bigint REFERENCES tool_types(id)"))

(defn- associate-deployed-components-with-tool-types
  "Associates existing deployed components with tool types."
  []
  (println "\t* associating existing deployed components with tool types")
  (update :deployed_components
          (set-fields
           {:tool_type_id (subselect :tool_types
                                     (fields :id)
                                     (where {:deployed_components.type
                                             :tool_types.name}))}))
  (update :deployed_components
          (set-fields
           {:tool_type_id (subselect :tool_types
                                     (fields :id)
                                     (where {:name "executable"}))})
          (where {:tool_type_id nil})))

(defn- remove-deployed-component-type
  "Removes the type column from the deployed_components table."
  []
  (println "\t* removing the type column from the deployed_components table")
  (exec-raw "ALTER TABLE deployed_components DROP COLUMN type"))

(defn- add-tool-type-id-constraints
  "Adds constraints to the tool_type_id column of the deployed_components
   table."
  []
  (println "\t* adding a not-null constraint to the tool_type_id column")
  (exec-raw
   "ALTER TABLE deployed_components ALTER COLUMN tool_type_id SET NOT NULL"))

(defn- redefine-analysis-job-types-view
  "Redefines the analysis_job_types view so that it retrieves the job type from
   the tool_types table."
  []
  (println "\t* redefining the analysis_job_types view")
  (exec-raw
   "CREATE VIEW analysis_job_types AS
    SELECT
        a.hid AS analysis_id,
        tt.name AS job_type
    FROM transformation_activity a
        JOIN transformation_task_steps tts ON a.hid = tts.transformation_task_id
        JOIN transformation_steps ts ON tts.transformation_step_id = ts.id
        JOIN transformations tx ON ts.transformation_id = tx.id
        JOIN template t ON tx.template_id::text = t.id::text
        JOIN deployed_components dc ON t.component_id::text = dc.id::text
        JOIN tool_types tt ON dc.tool_type_id = tt.id"))

(defn- redefine-analysis-listing-view
  "Redefines the analysis_listing view so that it retrieves the job type from
   the tool-types table."
  []
  (println "\t* redefining the analysis_listing view")
  (exec-raw
   "CREATE VIEW analysis_listing AS
    SELECT analysis.hid,
           analysis.id,
           analysis.\"name\",
           analysis.description,
           integration.integrator_name,
           integration.integrator_email,
           analysis.integration_date,
           analysis.edited_date,
           analysis.wikiurl,
           CAST(COALESCE(AVG(ratings.rating), 0.0) AS DOUBLE PRECISION)
               AS average_rating,
           EXISTS (
               SELECT *
               FROM template_group_template tgt
               JOIN template_group tg ON tgt.template_group_id = tg.hid
               JOIN workspace w ON tg.workspace_id = w.id
               WHERE analysis.hid = tgt.template_id
               AND w.is_public IS TRUE
           ) AS is_public, (
               SELECT COUNT(*)
               FROM transformation_task_steps tts
               WHERE tts.transformation_task_id = analysis.hid
           ) AS step_count,
           analysis.deleted,
           analysis.disabled,
           CASE WHEN COUNT(DISTINCT tt.name) = 0 THEN 'unknown'
                WHEN COUNT(DISTINCT tt.name) > 1 THEN 'mixed'
                ELSE MAX(tt.name)
           END AS overall_job_type
    FROM transformation_activity analysis
         LEFT JOIN integration_data integration
             ON analysis.integration_data_id = integration.id
         LEFT JOIN ratings ON analysis.hid = ratings.transformation_activity_id
         LEFT JOIN transformation_task_steps tts
             ON analysis.hid = tts.transformation_task_id
         LEFT JOIN transformation_steps ts ON tts.transformation_step_id = ts.id
         LEFT JOIN transformations tx ON ts.transformation_id = tx.id
         LEFT JOIN template t ON tx.template_id = t.id
         LEFT JOIN deployed_components dc ON t.component_id = dc.id
         LEFT JOIN tool_types tt ON dc.tool_type_id = tt.id
    GROUP BY analysis.hid,
             analysis.id,
             analysis.\"name\",
             analysis.description,
             integration.integrator_name,
             integration.integrator_email,
             analysis.integration_date,
             analysis.edited_date,
             analysis.wikiurl,
             analysis.deleted,
             analysis.disabled"))

(defn- redefine-deployed-component-listing-view
  "Redefines the deployed_component_listing view so that it retrieves the
   deployed component type from the tool_types table."
  []
  (println "\t* redefining the deployed_component_listing view")
  (exec-raw
   "CREATE VIEW deployed_component_listing AS
    SELECT row_number() OVER (ORDER BY analysis.hid, tts.hid) AS id,
           analysis.hid AS analysis_id,
           tts.hid AS execution_order,
           dc.hid AS deployed_component_hid,
           dc.id AS deployed_component_id,
           dc.\"name\",
           dc.description,
           dc.location,
           tt.\"name\" AS \"type\",
           dc.version,
           dc.attribution
    FROM transformation_activity analysis
         JOIN transformation_task_steps tts
             ON analysis.hid = tts.transformation_task_id
         JOIN transformation_steps ts ON tts.transformation_step_id = ts.id
         JOIN transformations tx ON ts.transformation_id = tx.id
         JOIN template t ON tx.template_id = t.id
         JOIN deployed_components dc ON t.component_id = dc.id
         JOIN tool_types tt ON dc.tool_type_id = tt.id"))

(defn- add-rule-value-association
  "Associates a rule type with a value type."
  [rt-name vt-hid]
  (insert :rule_type_value_type
          (values {:rule_type_id  (subselect :rule_type
                                             (fields :hid)
                                             (where {:name rt-name}))
                   :value_type_id vt-hid})))

(defn- add-environment-variable-types
  "Adds the property type and value type that will be used to support
   environment variables."
  []
  (println "\t* adding property and value types for environment variables")
  (let [vt-desc "An environment variable that is set before running a job"]
    (insert :value_type
            (values {:hid         6
                     :id          "96DE7B1E-FE29-468F-85C0-A9458CE66FB1"
                     :name        "EnvironmentVariable"
                     :description vt-desc})))
  (let [pt-desc "An environment variable that is set before running a job"]
    (insert :property_type
            (values {:hid           19
                     :id            "A024716E-1F18-4AF7-B59E-0745786D1B69"
                     :name          "EnvironmentVariable"
                     :description   pt-desc
                     :label         nil
                     :deprecated    false
                     :display_order 999
                     :value_type_id 6})))
  (dorun (map #(add-rule-value-association % 6)
              ["Regex" "CharacterLimit" "MustContain"])))

(defn- tpa-query-base
  "Generates the base query used to find the values for tool-property
   associations."
  []
  (-> (select* :tool_types)
      (fields [:tool_types.id :tool_type_id]
              [:property_type.hid :property_type_id])
      (join :property_type true)
      (order [:tool_types.id :property_type.hid])))

(defn- insert-tool-property-associations
  "Inserts tool/property associations into the database."
  [associations]
  (dorun (map #(insert :tool_type_property_type
                       (values {:tool_type_id     (:tool_type_id %)
                                :property_type_id (:property_type_id %)}))
              associations)))

(defn- add-tool-property-associations
  "Associates tool types with property types so that the Tito UI can determine
   which property types can be used for a selected deployed component."
  []
  (println "\t* associating tool types with property types")
  (exec-raw
   "CREATE TABLE tool_type_property_type (
       tool_type_id bigint NOT NULL REFERENCES tool_types(id),
       property_type_id bigint NOT NULL REFERENCES property_type(hid))")
  (insert-tool-property-associations
   (-> (tpa-query-base)
       (where {:tool_types.name "executable"})
       (select)))
  (insert-tool-property-associations
   (-> (tpa-query-base)
       (where {:tool_types.name    "fAPI"
               :property_type.name [not= "EnvironmentVariable"]})
       (select))))

(defn convert
  "Performs the conversions for database version 1.4.0:20120726.01."
  []
  (println "Performing conversion for" version)
  (drop-component-listing-views)
  (add-tool-types-table)
  (populate-tool-types-table)
  (add-tool-type-id-to-deployed-components)
  (associate-deployed-components-with-tool-types)
  (remove-deployed-component-type)
  (add-tool-type-id-constraints)
  (redefine-analysis-job-types-view)
  (redefine-analysis-listing-view)
  (redefine-deployed-component-listing-view)
  (add-environment-variable-types)
  (add-tool-property-associations))

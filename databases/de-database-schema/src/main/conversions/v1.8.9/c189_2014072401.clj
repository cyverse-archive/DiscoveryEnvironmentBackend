(ns facepalm.c189-2014072401
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20140724.01")

(defn- update-app-count-function
  "Adds a two-argument version of the app_count function that allows us to
   indicate whether or not apps containing external steps should be included
   in the count."
  []
  (println "\t* adding a two-argument version of the app_count function")
  (exec-raw
   "CREATE OR REPLACE FUNCTION app_count(bigint, boolean) RETURNS bigint AS $$
        SELECT COUNT(DISTINCT a.hid) FROM transformation_activity a
        JOIN template_group_template tgt ON a.hid = tgt.template_id
        WHERE NOT a.deleted
        AND tgt.template_group_id IN (SELECT * FROM app_group_hierarchy_ids($1))
        AND CASE
            WHEN $2 THEN TRUE
            ELSE NOT EXISTS (
                SELECT * FROM transformation_task_steps tts
                JOIN transformation_steps ts ON tts.transformation_step_id = ts.id
                JOIN transformations tx ON ts.transformation_id = tx.id
                WHERE tts.transformation_task_id = a.hid
                AND tx.external_app_id IS NOT NULL
            )
        END
    $$ LANGUAGE SQL"))

(defn- update-analysis-group-hierarchy-function
  "Adds a two-argument verison of the analysis_group_hierarchy function that
   allows the caller to indicate whether or not apps containing external steps
   should be included in app counts."
  []
  (println "\t* adding a two-argument version of the analysis_group_hierarchy function")
  (exec-raw
   "CREATE OR REPLACE FUNCTION analysis_group_hierarchy(bigint, boolean)
    RETURNS
    TABLE(
        parent_hid bigint,
        hid bigint,
        id varchar(255),
        name varchar(255),
        description varchar(255),
        workspace_id bigint,
        is_public boolean,
        app_count bigint
    ) AS $$
        WITH RECURSIVE subcategories AS
        (
                SELECT tgg.parent_group_id AS parent_hid, ag.hid, ag.id, ag.name,
                       ag.description, ag.workspace_id, ag.is_public,
                       app_count(ag.hid, $2) AS app_count
                FROM template_group_group tgg
                RIGHT JOIN analysis_group_listing ag ON tgg.subgroup_id = ag.hid
                WHERE ag.hid = $1
            UNION ALL
                SELECT tgg.parent_group_id AS parent_hid, ag.hid, ag.id, ag.name,
                       ag.description, ag.workspace_id, ag.is_public,
                       app_count(ag.hid, $2) AS app_count
                FROM subcategories sc, template_group_group tgg
                JOIN analysis_group_listing ag ON tgg.subgroup_id = ag.hid
                WHERE tgg.parent_group_id = sc.hid
        )
        SELECT * FROM subcategories
    $$ LANGUAGE SQL"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing conversion for" version)
  (update-app-count-function)
  (update-analysis-group-hierarchy-function))

(ns facepalm.c140-2012072001
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.4.0:20120720.01")

(defn- create-app-group-hierarchy-ids-fn
  "Creates the SQL function used to retrieve all of the internal app group
   identifiers rooted at the app group with the given internal identifier."
  []
  (println "\t* creating SQL function app_group_hierarchy_ids(bigint)")
  (exec-raw
   "CREATE FUNCTION app_group_hierarchy_ids(bigint)
            RETURNS TABLE(hid bigint) AS $$
        WITH RECURSIVE subcategories(parent_hid) AS
        (
                SELECT tgg.parent_group_id AS parent_hid, tg.hid
                FROM template_group_group tgg
                RIGHT JOIN template_group tg ON tgg.subgroup_id = tg.hid
                WHERE tg.hid = $1
            UNION ALL
                SELECT tgg.parent_group_id AS parent_hid, tg.hid
                FROM subcategories sc, template_group_group tgg
                JOIN template_group tg ON tgg.subgroup_id = tg.hid
                WHERE tgg.parent_group_id = sc.hid
        )
        SELECT hid FROM subcategories
    $$ LANGUAGE SQL"))

(defn- create-app-count-fn
  "Creates the SQL function used to obtain the number of apps in an app group
   and all of its descendents."
  []
  (println "\t* creating SQL function app_count(bigint)")
  (exec-raw
   "CREATE FUNCTION app_count(bigint) RETURNS bigint AS $$
        SELECT COUNT(a.hid) FROM transformation_activity a
        JOIN template_group_template tgt on a.hid = tgt.template_id
        WHERE NOT a.deleted
        AND tgt.template_group_id in (SELECT * FROM app_group_hierarchy_ids($1))
    $$ LANGUAGE SQL"))

(defn- create-analysis-group-hierarchy-fn
  "Creates the SQL function used to retrieve a listing of all app groups in
   the app group hierarchy rooted at the app group with the given identifier."
  []
  (println "\t* creating SQL function analysis_group_hierarchy(bigint)")
  (exec-raw
   "CREATE FUNCTION analysis_group_hierarchy(bigint)
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
                       app_count(ag.hid) AS app_count
                FROM template_group_group tgg
                RIGHT JOIN analysis_group_listing ag ON tgg.subgroup_id = ag.hid
                WHERE ag.hid = $1
            UNION ALL
                SELECT tgg.parent_group_id AS parent_hid, ag.hid, ag.id, ag.name,
                       ag.description, ag.workspace_id, ag.is_public,
                       app_count(ag.hid) AS app_count
                FROM subcategories sc, template_group_group tgg
                JOIN analysis_group_listing ag ON tgg.subgroup_id = ag.hid
                WHERE tgg.parent_group_id = sc.hid
        )
        SELECT * FROM subcategories
    $$ LANGUAGE SQL;"))

(defn convert
  "Performs the conversions for database version 1.40:20120720.01."
  []
  (println "Performing conversion for" version)
  (create-app-group-hierarchy-ids-fn)
  (create-app-count-fn)
  (create-analysis-group-hierarchy-fn))

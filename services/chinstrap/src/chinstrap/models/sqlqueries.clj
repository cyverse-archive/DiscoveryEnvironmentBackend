(ns chinstrap.models.sqlqueries
  (:use [kameleon.entities]
        [korma.core]
        [chinstrap.db])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]))

(defn all-app-count
  "Returns a count of all the queried deployed components in the DB."
  []
  (second (ffirst
    (select deployed_components
      (aggregate (count :name) :all)))))

(defn unused-app-count
  "Returns a count of components that are unused or are used in private or deleted apps" []
  (second (ffirst
    (exec-raw
      ["SELECT COUNT(dc.name)
        FROM deployed_components dc
        WHERE NOT EXISTS (
          SELECT t.id FROM template t
          LEFT JOIN transformations tx ON t.id = tx.template_id
          LEFT JOIN transformation_steps ts ON tx.id = ts.transformation_id
          LEFT JOIN transformation_task_steps tts ON ts.id = tts.transformation_step_id
          LEFT JOIN transformation_activity a ON tts.transformation_task_id = a.hid
          LEFT JOIN template_group_template tgt ON a.hid = tgt.template_id
          LEFT JOIN template_group tg ON tgt.template_group_id = tg.hid
          LEFT JOIN workspace w ON tg.workspace_id = w.id
          WHERE t.component_id = dc.id
          AND a.deleted IS FALSE
          AND w.is_public IS TRUE);"] :results))))

(defn used-app-count
  "Returns a count of all components that are used in public apps in the DB" []
  (second (ffirst
    (exec-raw
      ["SELECT COUNT(DISTINCT dc.name)
        FROM deployed_components dc
          LEFT JOIN template t ON dc.id = t.component_id
          LEFT JOIN transformations tx ON t.id = tx.template_id
          LEFT JOIN transformation_steps ts ON tx.id = ts.transformation_id
          LEFT JOIN transformation_task_steps tts ON ts.id = tts.transformation_step_id
          LEFT JOIN transformation_activity a ON tts.transformation_task_id = a.hid
          LEFT JOIN template_group_template tgt ON a.hid = tgt.template_id
          LEFT JOIN template_group tg ON tgt.template_group_id = tg.hid
          LEFT JOIN workspace w ON tg.workspace_id = w.id
        WHERE w.is_public IS TRUE
        AND a.deleted IS FALSE
        AND t.component_id IS NOT NULL;"] :results))))

(defn unused-app-list
  "Returns a list of all the deployed components in the DB that do not have
   associated transformation activities."  []
  (exec-raw
    ["SELECT DISTINCT dc.name, dc.version,
        ind.integrator_name,
        ind.integrator_email AS email
      FROM deployed_components dc
      LEFT JOIN integration_data ind ON ind.id = dc.integration_data_id
      WHERE NOT EXISTS (
        SELECT t.id FROM template t
        LEFT JOIN transformations tx ON t.id = tx.template_id
        LEFT JOIN transformation_steps ts ON tx.id = ts.transformation_id
        LEFT JOIN transformation_task_steps tts ON ts.id = tts.transformation_step_id
        LEFT JOIN transformation_activity a ON tts.transformation_task_id = a.hid
        LEFT JOIN template_group_template tgt ON a.hid = tgt.template_id
        LEFT JOIN template_group tg ON tgt.template_group_id = tg.hid
        LEFT JOIN workspace w ON tg.workspace_id = w.id
        WHERE t.component_id = dc.id
        AND a.deleted IS FALSE
        AND w.is_public IS TRUE)
      ORDER BY dc.name ASC;"] :results))

(defn integrator-list
   "Returns a list of all users with public apps and aggregates a count of
   them so that they can be ranked according to #'s of apps." []
  (exec-raw
    ["SELECT COUNT(ind.integrator_name) count,
      ind.integrator_name AS name,
      ind.integrator_email AS email,
      ind.id
      FROM deployed_components dc
      LEFT JOIN template t
        ON dc.id = t.component_id
      LEFT JOIN transformations tx
        ON t.id = tx.template_id
      LEFT JOIN transformation_steps ts
        ON tx.id = ts.transformation_id
      LEFT JOIN transformation_task_steps tts
        ON ts.id = tts.transformation_step_id
      LEFT JOIN transformation_activity a
        ON tts.transformation_task_id = a.hid
      LEFT JOIN integration_data ind
        ON a.integration_data_id = ind.id
      WHERE t.component_id IS NOT NULL
      AND EXISTS (
        SELECT * FROM template_group_template tgt
        LEFT JOIN template_group tg
          ON tgt.template_group_id = tg.hid
        LEFT JOIN workspace w ON tg.workspace_id = w.id
        WHERE w.is_public IS TRUE
        AND tgt.template_id = a.hid
      )
      GROUP BY integrator_name, integrator_email, ind.id
      ORDER BY count DESC, name ASC;"] :results))

(defn integrator-data
   "This query returns specific data about an integrator."
  [id]
  (select "integration_data"
    (where {:id (read-string id)})))

(defn integrator-details
"This function queries for useful integrator info based on the passed
 integrator id."
  [id]
    (exec-raw
      ["SELECT al.*
        FROM analysis_listing al
        LEFT JOIN integration_data ind
        ON al.integrator_email = ind.integrator_email
        WHERE al.is_public = true
        AND al.disabled = false
        AND al.deleted = false
        AND ind.id = ?
        ORDER BY al.average_rating DESC"
        [(read-string id)]] :results))

(defn count-apps
  "This function takes a collection of analysis_ids and queries the postgres
  database to return a count of tools run on that day."
  [ids]
  (select "template"
    (aggregate (count :name) :count :name)
    (fields :name)
    (where {:name [in
      (map #(subselect "template"
        (fields :name)
        (where {:id %}))ids )]})
    (order :count :desc)))

(defn historical-first-bucket []
  "This query returns a count of apps on the DE earlier than January 2012."
  (select "analysis_listing"
    (aggregate (count :*) "earlier_than_2012:")
    (where (and
      (= :is_public true)
      (< :integration_date (java.sql.Timestamp/valueOf "2012-01-01 00:00:00"))))))

(defn historical-second-bucket []
  "This query returns a count of apps on the DE from January to April 2012."
  (select "analysis_listing"
    (aggregate (count :*) "from_january_to_april_2012:")
    (where (and
      (= :is_public true)
      (>= :integration_date (java.sql.Timestamp/valueOf "2012-01-01 00:00:00"))
      (< :integration_date (java.sql.Timestamp/valueOf "2012-04-01 00:00:00"))))))

(defn historical-third-bucket []
  "This query returns a count of apps on the DE from April to July 2012."
  (select "analysis_listing"
    (aggregate (count :*) "from_april_to_july_2012:")
    (where (and
      (= :is_public true)
      (>= :integration_date (java.sql.Timestamp/valueOf "2012-04-01 00:00:00"))
      (< :integration_date (java.sql.Timestamp/valueOf "2012-07-01 00:00:00"))))))

(defn historical-last-bucket []
  "This query returns a count of apps on the DE after to july 2012."
  (select "analysis_listing"
    (aggregate (count :*) "after_july_2012:")
    (where (and
      (= :is_public true)
      (>= :integration_date (java.sql.Timestamp/valueOf "2012-07-01 00:00:00"))))))

(defn accumulated-second-bucket []
  "This query returns a cumulative count of apps on the DE up to April 2012."
  (select "analysis_listing"
    (aggregate (count :*) "to_april_2012:")
    (where (and
      (= :is_public true)
      (< :integration_date (java.sql.Timestamp/valueOf "2012-04-01 00:00:00"))))))

(defn accumulated-third-bucket []
  "This query returns a cumulative count of apps on the DE up to july 2012."
  (select "analysis_listing"
    (aggregate (count :*) "to_july_2012:")
    (where (and
      (= :is_public true)
      (< :integration_date (java.sql.Timestamp/valueOf "2012-07-01 00:00:00"))))))

(defn accumulated-last-bucket []
  "This query returns a cumulative count of apps on the DE."
  (select "analysis_listing"
    (aggregate (count :*) "after_july_2012:")
    (where (and
      (= :is_public true)
      (not= :integration_date nil)))))

(defn accumulated-app-count []
"This function combines functions to return a string ready for JSON formatting."
  (string/replace
      (string/replace
        (str
          (ffirst (historical-first-bucket))
          (ffirst (accumulated-second-bucket))
          (ffirst (accumulated-third-bucket))
          (ffirst (accumulated-last-bucket)))
      "[" "{")
      "]" "}"))

(defn historical-app-count []
"This function combines functions to return a string ready for JSON formatting."
  (string/replace
      (string/replace
        (str
          (ffirst (historical-first-bucket))
          (ffirst (historical-second-bucket))
          (ffirst (historical-third-bucket))
          (ffirst (historical-last-bucket)))
      "[" "{")
      "]" "}"))

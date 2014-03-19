(ns conrad.app-crud
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log])
  (:import [java.sql Timestamp]))

(defn load-deployed-components-for-app [app-hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM deployed_component_listing
      WHERE analysis_id = ?
      ORDER BY execution_order" app-hid]
    (doall rs)))

(defn load-public-categories-for-app [app-hid]
  (jdbc/with-query-results rs
    ["SELECT tg.* FROM template_group_template tgt
      JOIN template_group tg ON tgt.template_group_id = tg.hid
      JOIN workspace w ON tg.workspace_id = w.id
      WHERE w.is_public
      AND tgt.template_id = ?" app-hid]
    (doall (map #(dissoc % :hid) rs))))

(defn list-app [hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM analysis_listing WHERE hid = ?" hid]
    (first rs)))

(defn list-app-by-id [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM analysis_listing WHERE id = ?" id]
    (first rs)))

(defn list-deleted-and-orphaned-apps []
  (jdbc/with-query-results rs
    ["SELECT * FROM analysis_listing a
      WHERE (a.deleted AND EXISTS (
              SELECT * FROM template_group_template tgt
              JOIN template_group tg ON tgt.template_group_id = tg.hid
              JOIN workspace w ON tg.workspace_id = w.id
              WHERE tgt.template_id = a.hid AND w.is_public))
          OR NOT EXISTS (
              SELECT * FROM template_group_template tgt
              WHERE a.hid = tgt.template_id)"]
    (doall rs)))

(defn load-app-by-id [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM transformation_activity WHERE id = ?" id]
    (first rs)))

(defn- sql-timestamp [time]
  (if (or (nil? time) (= 0 time)) nil (Timestamp. time)))

(defn- convert-integration-date [app-update]
  (assoc app-update
    :integration_date (sql-timestamp (:integration_date app-update))))

(defn update-transformation-activity [app-update id]
  (jdbc/update-values :transformation_activity ["id = ?" id]
                      (convert-integration-date app-update)))

(defn add-integration-datum [integrator-name integrator-email]
  (jdbc/insert-values
   :integration_data
   [:integrator_name :integrator_email]
   [integrator-name integrator-email]))

(defn get-integration-data-id [integrator-name integrator-email]
  (jdbc/with-query-results rs
    ["SELECT * FROM integration_data
      WHERE integrator_name = ?
      AND integrator_email = ?"
     integrator-name integrator-email]
    (if (= (count rs) 0)
      (:id (add-integration-datum integrator-name integrator-email))
      (-> rs first :id))))

(defn set-app-deleted-flag [id flag]
  (log/debug (str "setting the deleted flag for app, " id " to " flag))
  (jdbc/update-values
   :transformation_activity ["id = ?" id]
   {:deleted flag}))

(defn- remove-public-categorizations [app-hid]
  (jdbc/delete-rows
   :template_group_template
   ["template_id = ? AND template_group_id IN (
         SELECT tg.hid FROM template_group tg
         JOIN workspace w ON tg.workspace_id = w.id
         WHERE w.is_public IS TRUE
     )" app-hid]))

(defn- categorize-app [app-hid category-hid]
  (jdbc/insert-values
   :template_group_template
   [:template_id :template_group_id]
   [app-hid category-hid]))

(defn move-public-app [app-hid category-hid]
  (remove-public-categorizations app-hid)
  (categorize-app app-hid category-hid))

(defn load-suggested-categories-for-app [app-hid]
  (jdbc/with-query-results rs
    ["SELECT tg.* FROM suggested_groups sg
      JOIN template_group tg ON sg.template_group_id = tg.hid
      WHERE sg.transformation_activity_id = ?" app-hid]
    (doall (map #(dissoc % :hid) rs))))

(ns conrad.app-admin
  (:use [conrad.common]
        [conrad.database]
        [conrad.app-listings]
        [conrad.app-crud]
        [conrad.category-crud]
        [conrad.category-listings])
  (:require [clojure.java.jdbc :as jdbc]
	    [clojure-commons.json :as cc-json]))

(defn- load-transformation-activity [id]
  (let [transformation-activity (load-app-by-id id)]
    (if (nil? transformation-activity)
      (throw (IllegalArgumentException. (str "app, " id ", not found"))))
    transformation-activity))

(defn- app-info->app-update [app-info integration-data-id]
  {:name (:name app-info)
   :description (:description app-info)
   :integration_date (force-long (:integration_date app-info) :integration_date)
   :integration_data_id integration-data-id
   :wikiurl (:wiki_url app-info)
   :disabled (:disabled app-info false)})

(defn- app-info->integration-data-id [{:keys [integrator_name integrator_email]}]
  (get-integration-data-id integrator_name integrator_email))

(defn- update-app-info [app-info]
  (let [id (:id app-info)
        transformation-activity (load-transformation-activity id)
        hid (:hid transformation-activity)
        integration-data-id (app-info->integration-data-id app-info)]
    (update-transformation-activity (app-info->app-update app-info integration-data-id) id)
    (success-response {:application (load-app-listing hid)})))

(defn- move-app-to-category [app category-id]
  (let [category (load-category-by-id category-id)
        app-hid (:hid app)
        category-hid (:hid category)]
    (ensure-category-doesnt-contain-subcategories category-id category-hid)
    (move-public-app app-hid category-hid)
    (set-app-deleted-flag (:id app) false)))

(defn- move-app* [categorization-info]
  (let [app-id (extract-required-field categorization-info :id)
        category-id (extract-required-field categorization-info :categoryId)
        app (load-transformation-activity app-id)]
    (if (= category-id trash-category-id)
      (set-app-deleted-flag app-id true)
      (move-app-to-category app category-id))
    (success-response {:category (list-category-with-apps category-id)})))

(defn- ensure-app-not-orphaned [id hid]
  (if (is-orphaned-app hid)
    (throw (IllegalStateException. (str "app, " id ", is orphaned")))))

(defn update-app [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (update-app-info (cc-json/body->json body)))))

(defn delete-app [id]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction
     (load-transformation-activity id)
     (set-app-deleted-flag id true)
     (success-response {:id id}))))

(defn undelete-app [id]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction
     (let [app (load-transformation-activity id)
           hid (:hid app)]
       (ensure-app-not-orphaned id hid)
       (set-app-deleted-flag id false)
       (success-response
        {:id id
         :categories (load-public-categories-for-app hid)})))))

(defn move-app [body]
  (jdbc/with-connection (db-connection)
    (jdbc/transaction (move-app* (cc-json/body->json body)))))

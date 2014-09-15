(ns metadactyl.persistence.app-metadata
  "Persistence layer for app metadata."
  (:use [kameleon.entities]
        [korma.core]
        [metadactyl.util.assertions])
  (:require [metadactyl.persistence.app-metadata.relabel :as relabel]))

(defn get-app
  "Retrieves an app from the database."
  [app-id]
  (assert-not-nil
   [:app-id app-id]
   (first
     (select app_listing
             (fields :id
                     :name
                     :description
                     :integrator_email
                     :step_count)
             (where {:id app-id})))))

(defn update-app-labels
  "Updates the labels in an app."
  [req]
  (relabel/update-app-labels req))

(defn app-accessible-by
  "Obtains the list of users who can access an app."
  [app-id]
  (map :username
       (select [:apps :a]
               (join [:app_category_app :aca]
                     {:a.id :aca.app_id})
               (join [:app_categories :g]
                     {:aca.app_category_id :g.id})
               (join [:workspace :w]
                     {:g.workspace_id :w.id})
               (join [:users :u]
                     {:w.user_id :u.id})
               (fields :u.username)
               (where {:a.id app-id}))))

(defn delete-app
  "Marks an app as deleted in the metadata database."
  [app-id]
  (update :apps
          (set-fields {:deleted true})
          (where {:id app-id})))

(ns metadactyl.persistence.app-metadata
  "Persistence layer for app metadata."
  (:use [kameleon.entities]
        [korma.core]
        [metadactyl.util.assertions])
  (:require [metadactyl.persistence.app-metadata.relabel :as relabel]
            [metadactyl.persistence.app-metadata.delete :as delete]))

(defn get-app
  "Retrieves an app from the database."
  [app-id]
  (assert-not-nil
   [:app-id app-id]
   (first (select transformation_activity
                  (where {:id app-id})))))

(defn update-app-labels
  "Updates the labels in an app."
  [req app-hid]
  (relabel/update-app-labels req app-hid))

(defn app-accessible-by
  "Obtains the list of users who can access an app."
  [app-id]
  (map :username
       (select [:transformation_activity :a]
               (join [:template_group_template :tgt]
                     {:a.hid :tgt.template_id})
               (join [:template_group :tg]
                     {:tgt.template_group_id :tg.hid})
               (join [:workspace :w]
                     {:tg.workspace_id :w.id})
               (join [:users :u]
                     {:w.user_id :u.id})
               (fields :u.username)
               (where {:a.id app-id}))))

(defn permanently-delete-app
  "Permanently removes an app from the metadata database."
  [app-id]
  (delete/permanently-delete-app ((comp :hid get-app) app-id)))

(defn delete-app
  "Marks an app as deleted in the metadata database."
  [app-id]
  (update :transformation_activity
          (set-fields {:deleted true})
          (where {:id app-id})))

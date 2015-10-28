(ns metadactyl.persistence.workspace
  (:use [korma.core :exclude [update]]
        [korma.db :only [transaction]])
  (:require [kameleon.app-groups :as app-groups]
            [kameleon.queries :as queries]
            [metadactyl.util.config :as config]))

(defn get-workspace
  [username]
  (->> (select [:workspace :w]
               (join [:users :u] {:w.user_id :u.id})
               (fields :w.id :w.user_id :w.root_category_id :w.is_public)
               (where {:u.username username}))
       (first)))

(defn- create-root-app-category
  [workspace-id]
  (app-groups/create-app-group workspace-id {:name (config/workspace-root-app-category)}))

(defn- create-default-workspace-subcategories
  [workspace-id root-category-id]
  (->> (config/get-default-app-categories)
       (map (comp :id (partial app-groups/create-app-group workspace-id) (partial hash-map :name)))
       (map-indexed (partial app-groups/add-subgroup root-category-id))
       (dorun)))

(defn- add-root-app-category
  [{workspace-id :id :as workspace}]
  (let [{root-category-id :id} (create-root-app-category workspace-id)]
    (create-default-workspace-subcategories workspace-id root-category-id)
    (queries/set-workspace-root-app-group workspace-id root-category-id)))

(defn create-workspace
  [username]
  (transaction
    (-> (queries/get-user-id username)
        (queries/create-workspace)
        (add-root-app-category)))
  (get-workspace username))

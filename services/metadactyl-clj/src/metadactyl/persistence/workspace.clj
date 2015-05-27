(ns metadactyl.persistence.workspace
  (:use [korma.core]))

(defn get-workspace
  [username]
  (select [:workspace w]
          (join [:users u] {:w.user_id :u.id})
          (fields :w.id :w.user_id :w.root_category_id :w.is_public)
          (where {:u.username username})))

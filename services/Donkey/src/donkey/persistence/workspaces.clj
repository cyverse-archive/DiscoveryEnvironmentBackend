(ns donkey.persistence.workspaces
  (:use [korma.core]
        [korma.db :only [with-db]])
  (:require [donkey.util.db :as db]))

(defn workspace-for-user
  "Loads the workspace for a user."
  [username]
  (first
   (with-db db/de
     (select [:workspace :w]
             (join [:users :u] {:w.user_id :u.id})
             (fields :w.id :w.is_public)
             (where {:u.username username})))))

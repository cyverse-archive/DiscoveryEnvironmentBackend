(ns donkey.persistence.metadata
  (:use korma.core)
  (:require [korma.db :as korma]
            [donkey.util.db :as db]))


(defentity ^{:private true} tags)


(defn insert-user-tag
  [owner value description]
  (korma/with-db db/metadata
    (insert tags (values {:value       value
                          :description description
                          :owner_id    owner}))))

#_(defn update-user-tag
  [owner old-value updates]
  (korma/with-db db/metadata
    (update tags
      (set-fields updates)
      (where {:owner_id owner :value old-value}))))


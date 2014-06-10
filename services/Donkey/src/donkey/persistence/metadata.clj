(ns donkey.persistence.metadata
  (:use korma.core)
  (:require [korma.db :as korma]
            [donkey.util.db :as db])
  (:import [java.util UUID]))


(defentity ^{:private true} tags
  (entity-fields :id :owner_id :value :description))

(defn get-tags-by-value
  [owner value]
  (korma/with-db db/metadata
    (select tags
      (where {:owner_id owner :value [like value]}))))

(defn get-tag
  [tag-id]
  (korma/with-db db/metadata
    (select tags
      (where {:id (UUID/fromString tag-id)}))))

(defn insert-user-tag
  [owner value description]
  (korma/with-db db/metadata
    (insert tags (values {:value       value
                          :description description
                          :owner_id    owner}))))

(defn update-user-tag
  [tag-id updates]
  (korma/with-db db/metadata
    (update tags
      (set-fields updates)
      (where {:id (UUID/fromString tag-id)}))))

(ns donkey.services.metadata.comments
  (:require [donkey.persistence.metadata :as db]
            [donkey.util.service :as svc]
            [donkey.services.metadata.tags :as tags]))


(defn add-comment
  [fs-cfg user entry-id comment]
  (tags/validate-entry-accessible fs-cfg user entry-id)
  (let [comment-id (db/insert-comment user entry-id "data" comment)]
    (svc/success-response {:id comment-id})))


(defn list-comments
  [fs-cfg user entry-id]
  (tags/validate-entry-accessible fs-cfg user entry-id)
  (svc/success-response {:comments (db/select-comments entry-id)}))

(ns donkey.services.metadata.comments
  (:require [clj-jargon.init :as fs-init]
            [clj-jargon.permissions :as fs-perm]
            [donkey.persistence.metadata :as db]
            [donkey.util.service :as svc]
            [donkey.services.filesystem.uuids :as uuid]
            [donkey.services.metadata.tags :as tags]))


(defn- prepare-post-time
  [comment]
  (assoc comment :post_time (.getTime (:post_time comment))))


(defn add-comment
  [fs-cfg user entry-id comment]
  (tags/validate-entry-accessible fs-cfg user entry-id)
  (let [comment (db/insert-comment user entry-id "data" comment)]
    (svc/success-response {:comment (prepare-post-time comment)})))


(defn list-comments
  [fs-cfg user entry-id]
  (tags/validate-entry-accessible fs-cfg user entry-id)
  (svc/success-response {:comments (map prepare-post-time (db/select-all-comments entry-id))}))


(defn update-retract-status
  [fs-cfg user entry-id comment-id retracting?]
  (fs-init/with-jargon fs-cfg [fs]
    (let [entry-path  (:path (uuid/path-for-uuid fs user (str entry-id)))
          owns-entry? (and entry-path (fs-perm/owns? fs user entry-path))
          comment     (db/select-comment comment-id)]
      (if (and entry-path comment)
        (if retracting?
          (if (or owns-entry? (= user (:owner_id comment)))
            (do
              (db/retract-comment comment-id user)
              (svc/success-response))
            (svc/donkey-response {} 403))
          (if (= user (:retracted_by comment))
            (do
              (db/readmit-comment comment-id)
              (svc/success-response))
            (svc/donkey-response {} 403)))
        (svc/donkey-response {} 404)))))

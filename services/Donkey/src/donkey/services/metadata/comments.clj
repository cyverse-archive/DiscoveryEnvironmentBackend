(ns donkey.services.metadata.comments
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes])
  (:require [cheshire.core :as json]
            [clj-jargon.init :as fs-init]
            [clj-jargon.permissions :as fs-perm]
            [donkey.auth.user-attributes :as user]
            [donkey.persistence.metadata :as db]
            [donkey.util.config :as config]
            [donkey.util.service :as svc]
            [donkey.services.filesystem.validators :as valid]
            [donkey.services.filesystem.uuids :as uuid])
  (:import [java.util UUID]
           [com.fasterxml.jackson.core JsonParseException]))


(defn- prepare-post-time
  [comment]
  (assoc comment :post_time (.getTime (:post_time comment))))


(defn- read-body
  [stream]
  (try+
    (slurp stream)
    (catch OutOfMemoryError _
      (throw+ {:error_code ERR_REQUEST_BODY_TOO_LARGE}))))


(defn- extract-entry-id
  [fs-cfg user entry-id-txt]
  (try+
    (let [entry-id (UUID/fromString entry-id-txt)]
      (fs-init/with-jargon fs-cfg [fs]
        (uuid/validate-uuid-accessible fs user entry-id))
      entry-id)
    (catch [:error_code ERR_DOES_NOT_EXIST] _ (throw+ {:error_code ERR_NOT_FOUND}))
    (catch IllegalArgumentException _ (throw+ {:error_code ERR_NOT_FOUND}))))


(defn add-comment
  "Adds a comment to an filesystem entry.

   Parameters:
     entry-id - the `entry-id` from the request. This should be the UUID corresponding to the entry
                being commented on
     body - the request body. It should be a JSON document containing the comment"
  [entry-id body]
  (try+
    (let [user     (:shortUsername user/current-user)
          entry-id (extract-entry-id (config/jargon-cfg) user entry-id)
          comment  (-> body read-body (json/parse-string true) :comment)]
      (when-not comment (throw+ {:error_code ERR_INVALID_JSON}))
      (let [comment (db/insert-comment  user entry-id "data" comment)]
        (svc/create-response {:comment (prepare-post-time comment)})))
    (catch JsonParseException _ (throw+ {:error_code ERR_INVALID_JSON}))))


(defn list-comments
  [entry-id]
  "Returns a list of comments attached to a given filesystem entry.

   Parameters:
     entry-id - the `entry-id` from the request. This should be the UUID corresponding to the entry
                being inspected"
  (let [entry-id (extract-entry-id (config/jargon-cfg) (:shortUsername user/current-user) entry-id)]
    (svc/success-response {:comments (map prepare-post-time (db/select-all-comments entry-id))})))


(defn update-retract-status
  [entry-id comment-id retracted]
  "Changes the retraction status for a given comment.

   Parameters:
     entry-id - the `entry-id` from the request. This should be the UUID corresponding to the entry
                owning the comment being modified
     comment-id - the comment-id from the request. This should be the UUID corresponding to the
                  comment being modified
     retracted - the `retracted` query parameter.  This should be either `true` or `false`."
  (fs-init/with-jargon (config/jargon-cfg) [fs]
    (let [user        (:shortUsername user/current-user)
          entry-id    (UUID/fromString entry-id)
          comment-id  (UUID/fromString comment-id)
          retracting? (Boolean/parseBoolean retracted)
          entry-path  (:path (uuid/path-for-uuid fs user (str entry-id)))
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

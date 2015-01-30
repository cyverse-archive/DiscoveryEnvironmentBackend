(ns donkey.services.metadata.comments
  (:use [korma.db :only [with-db]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [clojure-commons.error-codes :as err]
            [clojure-commons.validators :as validators]
            [donkey.auth.user-attributes :as user]
            [donkey.clients.data-info :as data]
            [donkey.persistence.metadata :as db]
            [donkey.services.filesystem.uuids :as data-uuids]
            [donkey.util.db :as db-util]
            [donkey.util.service :as svc]
            [donkey.util.validators :as valid]
            [kameleon.app-listing :as app-listing])
  (:import [com.fasterxml.jackson.core JsonParseException]))


(defn- extract-comment-id
  [entry-id comment-id-text]
  (try+
    (let [comment-id (valid/extract-uri-uuid comment-id-text)]
      (when-not (db/comment-on? comment-id entry-id)
        (throw+ {:error_code err/ERR_NOT_FOUND}))
      comment-id)))


(defn- extract-accessible-entry-id
  [user entry-id-txt]
  (try+
    (let [entry-id (valid/extract-uri-uuid entry-id-txt)]
      (data/validate-uuid-accessible user entry-id)
      entry-id)
    (catch [:error_code err/ERR_DOES_NOT_EXIST] _ (throw+ {:error_code err/ERR_NOT_FOUND}))))

(defn- extract-entry-id
  [entry-id-txt]
  (let [entry-id (valid/extract-uri-uuid entry-id-txt)]
    (when-not (data-uuids/uuid-exists? entry-id)
      (throw+ {:error_code err/ERR_NOT_FOUND :uuid entry-id}))
    entry-id))

(defn- extract-app-id
  [app-id]
  (let [app-uuid (valid/extract-uri-uuid app-id)]
    (with-db db-util/de
      (svc/assert-found (app-listing/get-app-listing app-uuid) "App" app-uuid))
    app-uuid))

(defn- extract-retracted
  [retracted-txt]
  (when-not retracted-txt
    (throw+ {:error_code err/ERR_MISSING_QUERY_PARAMETER :parameter "retracted"}))
  (if (coll? retracted-txt)
    (let [vals (set (map str/lower-case retracted-txt))]
      (when (> (count vals) 1) (throw+ {:error_code err/ERR_CONFLICTING_QUERY_PARAMETER_VALUES
                                        :parameter  "retracted"
                                        :values     vals}))
      (extract-retracted (first vals)))
    (case (str/lower-case retracted-txt)
      "true"  true
      "false" false
      (throw+ {:error_code err/ERR_BAD_QUERY_PARAMETER
               :parameter  "retracted"
               :value      retracted-txt}))))


(defn- prepare-comment
  [comment]
  (-> comment
    (dissoc :retracted_by)
    (assoc :post_time (.getTime (:post_time comment)))))


(defn- read-body
  [stream]
  (try+
    (slurp stream)
    (catch OutOfMemoryError _
      (throw+ {:error_code err/ERR_REQUEST_BODY_TOO_LARGE}))))


(defn add-comment
  "Adds a comment to a target with the given ID and type.

   Parameters:
     target-id - the UUID corresponding to the entry being commented on
     target-type - The type of target (`analysis`|`app`|`data`|`user`)
     body - the request body. It should be a JSON document containing the comment"
  [target-id target-type body]
  (try+
    (let [user    (:shortUsername user/current-user)
          comment (-> body read-body (json/parse-string true) :comment)]
      (when-not comment (throw+ {:error_code err/ERR_INVALID_JSON}))
      (svc/create-response {:comment (->> comment
                                       (db/insert-comment user target-id target-type)
                                       prepare-comment)}))
    (catch JsonParseException _ (throw+ {:error_code err/ERR_INVALID_JSON}))))

(defn add-data-comment
  "Adds a comment to a filesystem entry.

   Parameters:
     entry-id - the `entry-id` from the request. This should be the UUID corresponding to the entry
                being commented on
     body - the request body. It should be a JSON document containing the comment"
  [entry-id body]
  (let [user     (:shortUsername user/current-user)
        entry-id (extract-accessible-entry-id user entry-id)
        tgt-type (data/resolve-data-type entry-id)]
    (add-comment entry-id tgt-type body)))

(defn add-app-comment
  "Adds a comment to an App.

   Parameters:
     app-id - the UUID corresponding to the App being commented on
     body - the request body. It should be a JSON document containing the comment"
  [app-id body]
  (add-comment (extract-app-id app-id) "app" body))

(defn list-comments
  [target-id]
  "Returns a list of comments attached to a given target ID.

   Parameters:
     target-id - the UUID corresponding to the entry being inspected"
   (let [comments (map prepare-comment (db/select-all-comments target-id))]
     (svc/success-response {:comments comments})))

(defn list-data-comments
  [entry-id]
  "Returns a list of comments attached to a given filesystem entry.

   Parameters:
     entry-id - the `entry-id` from the request. This should be the UUID corresponding to the entry
                being inspected"
  (list-comments (extract-accessible-entry-id (:shortUsername user/current-user) entry-id)))

(defn list-app-comments
  [app-id]
  "Returns a list of comments attached to a given App ID.

   Parameters:
     app-id - the `app-id` from the request. This should be the UUID corresponding to the App being
              inspected"
  (list-comments (extract-app-id app-id)))

(defn update-retract-status
  [target-uuid comment-id retracted owns-target?]
  "Changes the retraction status for a given comment.

   Parameters:
     target-uuid - the UUID corresponding to the target owning the comment being modified
     comment-id - the UUID corresponding to the comment being modified
     retracted - Whether the user wants to retract the comment (should be either `true` or `false`)."
  (let [user         (:shortUsername user/current-user)
        retracting?  (extract-retracted retracted)
        comment-uuid (extract-comment-id target-uuid comment-id)
        comment      (db/select-comment comment-uuid)]
    (if retracting?
      (if (or owns-target? (= user (:commenter comment)))
        (db/retract-comment comment-uuid user)
        (throw+ {:error_code err/ERR_NOT_OWNER :reason "doesn't own either entry or comment"}))
      (if (= user (:retracted_by comment))
        (db/readmit-comment comment-uuid)
        (throw+ {:error_code err/ERR_NOT_OWNER :reason "wasn't retractor"})))
    (svc/success-response)))

(defn update-data-retract-status
  [entry-id comment-id retracted]
  "Changes the retraction status for a given comment.

   Parameters:
     entry-id - the `entry-id` from the request. This should be the UUID corresponding to the entry
                owning the comment being modified
     comment-id - the comment-id from the request. This should be the UUID corresponding to the
                  comment being modified
     retracted - the `retracted` query parameter. This should be either `true` or `false`."
  (let [user        (:shortUsername user/current-user)
        entry-uuid  (extract-accessible-entry-id user entry-id)
        entry-path  (:path (data/stat-by-uuid user entry-uuid))
        owns-entry? (and entry-path (data/owns? user entry-path))]
    (update-retract-status entry-uuid comment-id retracted owns-entry?)))

(defn update-app-retract-status
  [app-id comment-id retracted]
  "Changes the retraction status for a given comment.

   Parameters:
     app-id - the UUID corresponding to the App owning the comment being modified
     comment-id - the comment-id from the request. This should be the UUID corresponding to the
                  comment being modified
     retracted - the `retracted` query parameter. This should be either `true` or `false`."
  (let [app-uuid  (valid/extract-uri-uuid app-id)
        app       (with-db db-util/de
                    (svc/assert-found (app-listing/get-app-listing app-uuid) "App" app-uuid))
        owns-app? (validators/user-owns-app? user/current-user app)]
    (update-retract-status app-uuid comment-id retracted owns-app?)))

(defn delete-comment
  [target-uuid comment-id]
  (let [comment-uuid (extract-comment-id target-uuid comment-id)]
    (db/mark-comment-deleted comment-uuid true))
  (svc/successful-delete-response))

(defn delete-data-comment
  [entry-id comment-id]
  (delete-comment (extract-entry-id entry-id) comment-id))

(defn delete-app-comment
  [app-id comment-id]
  (delete-comment (extract-app-id app-id) comment-id))

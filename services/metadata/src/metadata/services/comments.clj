(ns metadata.services.comments
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as err]
            [clojure-commons.validators :as validators]
            [metadata.persistence.comments :as db]
            [metadata.util.service :as service]))


(defn- validate-comment-id
  [data-id comment-id]
  (when-not (db/comment-on? comment-id data-id)
    (throw+ {:error_code err/ERR_NOT_FOUND})))

(defn- prepare-comment
  [comment]
  (-> comment
      (dissoc :retracted_by)
      (assoc :post_time (.getTime (:post_time comment)))))


(defn add-comment
  "Adds a comment to a target with the given ID and type.

   Parameters:
     user - the user adding the comment (the comment owner)
     target-id - the UUID corresponding to the data item being commented on
     target-type - The type of target (`analysis`|`app`|`file`|`folder`|`user`)
     comment - the comment text"
  [user target-id target-type comment]
  {:comment (->> (db/insert-comment user target-id target-type comment)
                 prepare-comment)})

(defn add-data-comment
  "Adds a comment to a data item.

   Parameters:
     user - the user adding the comment (the comment owner)
     data-id - the `data-id` from the request. This should be the UUID corresponding to the data item 
               being commented on
     data-type - The type of target (`file`|`folder`)
     body - the request body. It should be a map containing the comment"
  [user data-id data-type {:keys [comment]}]
  (add-comment user data-id data-type comment))

(defn add-app-comment
  "Adds a comment to an App.

   Parameters:
     user - the user adding the comment (the comment owner)
     app-id - the UUID corresponding to the App being commented on
     body - the request body. It should be a map containing the comment"
  [user app-id {:keys [comment]}]
  (add-comment user app-id "app" comment))

(defn list-comments
  [target-id]
  "Returns a list of comments attached to a given target ID.

   Parameters:
     target-id - the UUID corresponding to the data item being inspected"
   (let [comments (map prepare-comment (db/select-all-comments target-id))]
     {:comments comments}))

(defn list-data-comments
  [data-id]
  "Returns a list of comments attached to a given data item.

   Parameters:
     data-id - the `data-id` from the request. This should be the UUID corresponding to the data item
                being inspected"
  (list-comments data-id))

(defn list-app-comments
  [app-id]
  "Returns a list of comments attached to a given App ID.

   Parameters:
     app-id - the `app-id` from the request. This should be the UUID corresponding to the App being
              inspected"
  (list-comments app-id))

(defn update-retract-status
  [user target-uuid comment-id retracting? target-admin?]
  "Changes the retraction status for a given comment.

   Parameters:
     user - the user updating the comment retraction
     target-uuid - the UUID corresponding to the target owning the comment being modified
     comment-id - the UUID corresponding to the comment being modified
     retracting? - Whether the user wants to retract the comment (should be a Boolean)
     target-admin? - Whether the user is considered an admin of the target (owner or admin user)."
  (validate-comment-id target-uuid comment-id)
  (let [comment (db/select-comment comment-id)]
    (if retracting?
      (if (or target-admin? (= user (:commenter comment)))
        (db/retract-comment comment-id user)
        (throw+ {:error_code err/ERR_NOT_OWNER :reason "doesn't own comment"}))
      (if (= user (:retracted_by comment))
        (db/readmit-comment comment-id)
        (throw+ {:error_code err/ERR_NOT_OWNER :reason "wasn't retractor"})))
    nil))

(defn update-data-retract-status
  [user data-id comment-id retracted]
  "Changes the retraction status for a given comment.

   Parameters:
     user - the user updating the comment retraction
     data-id - the `data-id` from the request. This should be the UUID corresponding to the data item 
                owning the comment being modified
     comment-id - the comment-id from the request. This should be the UUID corresponding to the
                  comment being modified
     retracted - the `retracted` query parameter. This should be a Boolean."
  (update-retract-status user data-id comment-id retracted false))

(defn update-app-retract-status
  [user app-id comment-id retracted]
  "Changes the retraction status for a given comment.

   Parameters:
     user - the user updating the comment retraction
     app-id - the UUID corresponding to the App owning the comment being modified
     comment-id - the comment-id from the request. This should be the UUID corresponding to the
                  comment being modified
     retracted - the `retracted` query parameter. This should be a Boolean."
  (update-retract-status user app-id comment-id retracted false))

(defn admin-update-retract-status
  [user target-id comment-id retracted]
  (update-retract-status user target-id comment-id retracted true))

(defn delete-comment
  [target-uuid comment-id]
  (validate-comment-id target-uuid comment-id)
  (db/mark-comment-deleted comment-id true)
  nil)

(defn delete-data-comment
  [data-id comment-id]
  (delete-comment data-id comment-id))

(defn delete-app-comment
  [app-id comment-id]
  (delete-comment app-id comment-id))

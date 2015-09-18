(ns metadata.persistence.comments
  (:use [korma.core :exclude [update]])
  (:require [kameleon.db :as db]
            [korma.core :as sql]))

(defn- fmt-comment
  [comment]
  (when comment
    {:id           (:id comment)
     :commenter    (:owner_id comment)
     :post_time    (:post_time comment)
     :retracted    (:retracted comment)
     :retracted_by (:retracted_by comment)
     :comment      (:value comment)}))

(defn insert-comment
  "Inserts a comment into the comments table.

   Parameters:
     owner       - The authenticated user making the comment
     target-id   - The UUID of the thing being commented on
     target-type - The type of target (`analysis`|`app`|`data`|`user`)
     comment     - The comment

   Returns:
     It returns a comment resource"
  [owner target-id target-type comment]
  (fmt-comment
    (insert :comments
      (values {:owner_id    owner
               :target_id   target-id
               :target_type (db/->enum-val target-type)
               :value       comment}))))

(defn comment-on?
  "Indicates whether or not a given comment was attached to a given target.

   Parameters:
     comment-id - The UUID of the comment
     target-id  - The UUID of the target

   Returns:
     It returns true if the comment is attached to the target, otherwise it returns false."
  [comment-id target-id]
  (-> (select :comments
        (aggregate (count :*) :cnt)
        (where {:id        comment-id
                :target_id target-id
                :deleted   false}))
    first :cnt pos?))

(defn select-comment
  "Retrieves a comment resource

    Parameters:
      comment-id - The UUID of the comment

    Returns:
      The comment resource or nil if comment-id isn't a comment that hasn't been deleted."
  [comment-id]
  (-> (select :comments (where {:id comment-id :deleted false}))
      first fmt-comment))

(defn select-all-comments
  "Retrieves all undeleted comments attached to a given target.

   Parameters:
     target-id - The UUID of the target of interest

   Returns:
     It returns a collection of comment resources attached to the target. If the target doesn't
     exist, an empty collection will be returned."
  [target-id]
  (map fmt-comment (select :comments (where {:target_id target-id :deleted false}))))

(defn retract-comment
  "Marks a comment as retracted. It assumes the retracting user is an authenticated user. If the
   comment doesn't exist, it silently fails.

   Parameters:
     comment-id      - The UUID of the comment being retracted
     retracting-user - The authenticated user retracting the comment."
  [comment-id retracting-user]
  (sql/update :comments
    (set-fields {:retracted true :retracted_by retracting-user})
    (where {:id comment-id}))
  nil)

(defn readmit-comment
  "Unmarks a comment as retracted. If the comment doesn't exist, it silently fails.

   Parameters:
     comment-id - The UUID of the comment being readmitted."
  [comment-id]
  (sql/update :comments
    (set-fields {:retracted false :retracted_by nil})
    (where {:id comment-id}))
  nil)

(defn mark-comment-deleted
  "Deletes a comment from the metadata database. If the comment doesn't exist, it silently fails.

   Parameters:
     comment-id - The UUID of the comment being deleted."
  [comment-id deleted?]
  (sql/update :comments
    (set-fields {:deleted deleted?})
    (where {:id comment-id})))

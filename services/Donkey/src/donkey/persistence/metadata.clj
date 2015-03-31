(ns donkey.persistence.metadata
  (:use korma.core)
  (:require [korma.db :as korma]
            [kameleon.db :as db])
  (:import [java.util UUID]
           [clojure.lang IPersistentMap ISeq]))


;; COMMENTS

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
  (fmt-comment (korma/with-db db/metadata
                 (insert :comments
                   (values {:owner_id    owner
                            :target_id   target-id
                            :target_type (db/->enum-val target-type)
                            :value       comment})))))

(defn comment-on?
  "Indicates whether or not a given comment was attached to a given target.

   Parameters:
     comment-id - The UUID of the comment
     target-id  - The UUID of the target

   Returns:
     It returns true if the comment is attached to the target, otherwise it returns false."
  [comment-id target-id]
  (-> (korma/with-db db/metadata
        (select :comments
          (aggregate (count :*) :cnt)
          (where {:id        comment-id
                  :target_id target-id
                  :deleted   false})))
    first :cnt pos?))

(defn select-comment
  "Retrieves a comment resource

    Parameters:
      comment-id - The UUID of the comment

    Returns:
      The comment resource or nil if comment-id isn't a comment that hasn't been deleted."
  [comment-id]
  (-> (korma/with-db db/metadata
        (select :comments (where {:id comment-id :deleted false})))
    first fmt-comment))

(defn select-all-comments
  "Retrieves all undeleted comments attached to a given target.

   Parameters:
     target-id - The UUID of the target of interest

   Returns:
     It returns a collection of comment resources attached to the target. If the target doesn't
     exist, an empty collection will be returned."
  [target-id]
  (map fmt-comment
       (korma/with-db db/metadata
         (select :comments (where {:target_id target-id :deleted false})))))

(defn retract-comment
  "Marks a comment as retracted. It assumes the retracting user is an authenticated user. If the
   comment doesn't exist, it silently fails.

   Parameters:
     comment-id      - The UUID of the comment being retracted
     retracting-user - The authenticated user retracting the comment."
  [comment-id retracting-user]
  (korma/with-db db/metadata
    (update :comments
      (set-fields {:retracted true :retracted_by retracting-user})
      (where {:id comment-id})))
  nil)

(defn readmit-comment
  "Unmarks a comment as retracted. If the comment doesn't exist, it silently fails.

   Parameters:
     comment-id - The UUID of the comment being readmitted."
  [comment-id]
  (korma/with-db db/metadata
    (update :comments
      (set-fields {:retracted false :retracted_by nil})
      (where {:id comment-id})))
  nil)

(defn mark-comment-deleted
  "Deletes a comment from the metadata database. If the comment doesn't exist, it silently fails.

   Parameters:
     comment-id - The UUID of the comment being deleted."
  [comment-id deleted?]
  (korma/with-db db/metadata
    (update :comments
      (set-fields {:deleted deleted?})
      (where {:id comment-id}))))


;; FAVORITES

(defn is-favorite?
  "Indicates whether or not given target is a favorite of the given authenticated user.

   Parameters:
     user      - the authenticated user name
     target-id - the UUID of the thing being marked as a user favorite

   Returns:
     It returns true if the give target has been marked as a favorite, otherwise it returns false.
     It also returns false if the user or target doesn't exist."
  [user ^UUID target-id]
  (-> (korma/with-db db/metadata
        (select :favorites
          (aggregate (count :*) :cnt)
          (where {:target_id target-id :owner_id user})))
    first :cnt pos?))

(defn select-favorites-of-type
  "Selects all targets of a given type that have are favorites of a given authenticated user.

   Parameters:
     user         - the authenticated user name
     target-types - the set of types of target may belong to
                    (`analysis`|`app`|`file`|`folder`|`user`)

   Returns:
     It returns a lazy sequence of favorite target UUIDs. If the user doesn't exist, the sequence
     will be empty."
  [user target-types]
  (map :target_id
       (korma/with-db db/metadata
         (select :favorites
           (fields :target_id)
           (where {:target_type [in (map db/->enum-val target-types)]
                   :owner_id    user})))))

(defn insert-favorite
  "Marks a given target as a favorite of the given authenticated user. It assumes the authenticated
   user exists and the target is of the indicated type.

   Parameters:
     user        - the authenticated user name
     target-id   - the UUID of the target
     target-type - the type of target (`analysis`|`app`|`data`|`user`)"
  [user target-id target-type]
  (korma/with-db db/metadata
    (insert :favorites
      (values {:target_id   target-id
               :target_type (db/->enum-val target-type)
               :owner_id    user})))
  nil)

(defn delete-favorite
  [user target-id]
  "Unmarks a given target as a favorite of the given authenticated user.

   Parameters:
     user      - the authenticated user name
     target-id - the UUID of the target"
  (korma/with-db db/metadata
    (delete :favorites (where {:target_id target-id :owner_id user})))
  nil)


;; TAGS

(defn filter-tags-owned-by-user
  [owner tag-ids]
  "Filters a set of tags for those owned by the given user.

   Parameters:
     owner   - the owner of the tags to keep
     tag-ids - the UUIDs of the tags to filter

   Returns:
     It returns a lazy sequence of tag UUIDs owned by the given user."
  (map :id
       (korma/with-db db/metadata
         (select :tags
           (fields :id)
           (where {:owner_id owner :id [in tag-ids]})))))

(defn get-tags-by-value
  "Retrieves up to a certain number of the tags owned by a given user that have a value matching a
   given pattern. If no max number is provided, all tags will be returned.

   Parameters:
     owner       - the owner of the tags to return
     value-glob  - the pattern of the value to match. A `%` means zero or more of any character. A
                   `_` means one of any character.
     max-results - (OPTIONAL) If this is provided, no more than this number of results will be
                   returned.

   Returns:
     A lazy sequence of tags."
  [owner value-glob & [max-results]]
  (let [query  (-> (select* :tags)
                 (fields :id :value :description)
                 (where (and {:owner_id owner}
                             (raw (str "lower(value) like lower('" value-glob "')")))))
        query' (if max-results
                 (-> query (limit max-results))
                 query)]
    (korma/with-db db/metadata (select query'))))

(defn get-tag-owner
  "Retrieves the user name of the owner of the given tag.

   Parameters:
     tag-id - The UUID of tag.

   Returns:
     The user name or nil if the tag doesn't exist."
  [tag-id]
  (-> (korma/with-db db/metadata
        (select :tags
          (fields :owner_id)
          (where {:id tag-id})))
    first :owner_id))

(defn ^IPersistentMap insert-user-tag
  "Inserts a user tag.

   Parameters:
     owner       - The user name of the owner of the new tag.
     value       - The value of the tag. It must be no more than 255 characters.
     description - The description of the tag. If nil, an empty string will be inserted.

   Returns:
     It returns the new database tag entry."
  [^String owner ^String value ^String description]
  (let [description (if description description "")]
    (korma/with-db db/metadata
      (insert :tags
        (values {:value       value
                 :description description
                 :owner_id    owner})))))


(defn ^IPersistentMap update-user-tag
  "Updates a user tag's description and/or value.

   Parameters:
     tag-id  - The UUID of the tag to update
     updates - A map containing the updates. The map may contain only the keys :value and
               :description. It doesn't need to contain both.  The :value key will map to a new
               value for the tag, and the :description key will map to a new description. A nil
               description will be converted to an empty string.

   Returns:
     It returns the updated tag record."
  [^UUID tag-id ^IPersistentMap updates]
  (let [updates (if (get updates :description :not-found)
                  updates
                  (assoc updates :description ""))]
    (korma/with-db db/metadata
      (update :tags
        (set-fields updates)
        (where {:id tag-id})))))


(defn delete-user-tag
  "This detaches a user tag from all metadata and deletes it.

   Parameters:
     tag-id - The UUID of the tag to delete."
  [tag-id]
  (korma/with-db db/metadata
    (delete :attached_tags (where {:tag_id tag-id}))
    (delete :tags (where {:id tag-id})))
  nil)

(defn select-attached-tags
  "Retrieves the set of tags a user has attached to something.

   Parameters:
     user      - the user name
     target-id - The UUID of the thing of interest

   Returns:
     It returns a lazy sequence of tag resources."
  [user target-id]
  (korma/with-db db/metadata
    (select :tags
      (fields :id :value :description)
      (where {:owner_id user
              :id       [in (subselect :attached_tags
                              (fields :tag_id)
                              (where {:target_id target-id :detached_on nil}))]}))))

(defn filter-attached-tags
  "Filter a set of tags for those attached to a given target.

   Parameters:
     target-id - the UUID of the target used to filter the tags
     tag-ids   - the set of tags to filter

   Returns:
     It returns a lazy sequence of tag UUIDs that have been filtered."
  [target-id tag-ids]
  (map :tag_id
       (korma/with-db db/metadata
         (select :attached_tags
           (fields :tag_id)
           (where {:target_id   target-id
                   :detached_on nil
                   :tag_id      [in tag-ids]})))))

(defn insert-attached-tags
  "Attach a set of user tags to a target.

   Parameters:
     attacher    - The user name of the user attaching the tags.
     target-id   - The UUID of target receiving tags
     target-type - the type of target (`analysis`|`app`|`data`|`user`)
     tag-ids     - the collection of tags to attach"
  [attacher target-id target-type tag-ids]
  (when-not (empty? tag-ids)
    (let [target-type (db/->enum-val target-type)
          new-values  (map #(hash-map :tag_id      %
                                      :target_id   target-id
                                      :target_type target-type
                                      :attacher_id attacher)
                           tag-ids)]
      (korma/with-db db/metadata
        (insert :attached_tags (values new-values)))
      nil)))

(defn mark-tags-detached
  "Detach a set of user tags from a target.

   Parameters:
     detacher  - The user name of the user detaching the tags.
     target-id - The UUID of the target having some of its tags removed.
     tag-ids   - the collection tags to detach"
  [detacher target-id tag-ids]
  (korma/with-db db/metadata
    (update :attached_tags
      (set-fields {:detacher_id detacher
                   :detached_on (sqlfn now)})
      (where {:target_id   target-id
              :detached_on nil
              :tag_id      [in tag-ids]})))
  nil)


(defn ^ISeq select-tag-targets
  "Retrieve all of the objects that have a given tag attached.

   Parameters:
     tag-id - The id of the tag

   Returns:
     It returns the attachment records for this tag."
  [^UUID tag-id]
  (korma/with-db db/metadata
    (select :attached_tags
      (where {:tag_id tag-id :detached_on nil}))))

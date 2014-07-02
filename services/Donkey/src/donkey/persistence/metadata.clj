(ns donkey.persistence.metadata
  (:use korma.core)
  (:require [korma.db :as korma]
            [donkey.util.db :as db])
  (:import [java.util UUID]))


(defn- ->enum-val
  [val]
  (raw (str \' val \')))


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
                            :target_type (->enum-val target-type)
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


;; FAVORITES

(defn is-favorite?
  "Indicates whether or not given target is a favorite of the given authenticated user.

   Parameters:
     user      - the authenticated user name
     target-id - the UUID of the thing being marked as a user favorite

   Returns:
     It returns true if the give target has been marked as a favorite, otherwise it returns false.
     It also returns false if the user or target doesn't exist."
  [user target-id]
  (-> (korma/with-db db/metadata
        (select :favorites
          (aggregate (count :*) :cnt)
          (where {:target_id target-id :owner_id user})))
    first :cnt pos?))

(defn select-favorites-of-type
  "Selects all targets of a given type that have are favorites of a given authenticated user.

   Parameters:
     user        - the authenticated user name
     target-type - the type of target (`analysis`|`app`|`data`|`user`)

   Returns:
     It returns a lazy sequence of favorite target UUIDs. If the user doesn't exist, the sequence
     will be empty."
  [user target-type]
  (map :target_id
       (korma/with-db db/metadata
         (select :favorites
           (fields :target_id)
           (where {:target_type (->enum-val target-type)
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
               :target_type (->enum-val target-type)
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
  (map :id
       (korma/with-db db/metadata
         (select :tags
           (fields :id)
           (where {:owner_id owner :id [in tag-ids]})))))

(defn get-tags-by-value
  [owner value & [max-results]]
  (let [query  (-> (select* :tags)
                   (fields :id :value :description)
                   (where {:owner_id owner :value [like value]}))
        query' (if max-results
                 (-> query
                     (limit max-results))
                 query)]
    (korma/with-db db/metadata
      (select query'))))

(defn get-tag
  [tag-id]
  (korma/with-db db/metadata
    (select :tags
      (where {:id (UUID/fromString tag-id)}))))

(defn insert-user-tag
  [owner value description]
  (korma/with-db db/metadata
    (insert :tags
      (values {:value       value
               :description description
               :owner_id    owner}))))

(defn update-user-tag
  [tag-id updates]
  (korma/with-db db/metadata
    (update :tags
      (set-fields updates)
      (where {:id (UUID/fromString tag-id)}))))

(defn select-attached-tags
  [user target-id]
  (korma/with-db db/metadata
    (select :tags
      (fields :id :value :description)
      (where {:owner_id user
              :id       [in (subselect :attached_tags
                              (fields :tag_id)
                              (where {:target_id   target-id
                                      :detached_on nil}))]}))))

(defn filter-attached-tags
  [target-id tag-ids]
  (map :tag_id
       (korma/with-db db/metadata
         (select :attached_tags
           (fields :tag_id)
           (where {:target_id   target-id
                   :detached_on nil
                   :tag_id      [in tag-ids]})))))

(defn insert-attached-tags
  [attacher target-id target-type tag-ids]
  (when-not (empty? tag-ids)
    (let [new-values (map #(hash-map :tag_id      %
                                     :target_id   target-id
                                     :target_type (->enum-val target-type)
                                     :attacher_id attacher)
                          tag-ids)]
      (korma/with-db db/metadata
        (insert :attached_tags (values new-values))))))

(defn mark-tags-detached
  [detacher target-id tag-ids]
  (korma/with-db db/metadata
    (update :attached_tags
      (set-fields {:detacher_id detacher
                   :detached_on (sqlfn now)})
      (where {:target_id   target-id
              :detached_on nil
              :tag_id      [in tag-ids]}))))


;; TEMPLATES

(defn find-existing-metadata-template-avu
  "Finds an existing AVU by ID or attribute, and by target_id and owner_id."
  [{avu-id :id, :as avu}]
  (let [id-key (if avu-id :id :attribute)]
    (korma/with-db db/metadata
      (first
       (select :avus
               (where (-> (select-keys avu [id-key :target_id :owner_id])
                          (assoc :target_type (->enum-val "data")))))))))

(defn get-avus-for-metadata-template
  "Gets AVUs for the given Metadata Template."
  [user-id data-id template-id]
  (korma/with-db db/metadata
    (select :avus
            (join [:template_instances :t]
                  {:t.avu_id :avus.id})
            (where {:t.template_id template-id
                    :avus.target_id data-id
                    :avus.target_type (->enum-val "data")
                    :avus.owner_id user-id}))))

(defn get-metadata-template-ids
  "Finds Metadata Template IDs associated with the given user's data item."
  [user-id data-id]
  (korma/with-db db/metadata
    (select [:template_instances :t]
            (fields :template_id)
            (join :avus {:t.avu_id :avus.id})
            (where {:avus.target_id data-id
                    :avus.target_type (->enum-val "data")
                    :avus.owner_id user-id})
            (group :template_id))))

(defn remove-avu-template-instances
  "Removes the given Metadata Template AVU associations."
  [template-id avu-ids]
  (korma/with-db db/metadata
    (delete :template_instances
            (where {:template_id template-id
                    :avu_id [in avu-ids]}))))

(defn add-template-instances
  "Associates the given AVU with the given Metadata Template ID."
  [template-id avu-ids]
  (korma/with-db db/metadata
    (korma/transaction
     (remove-avu-template-instances template-id avu-ids)
     (insert :template_instances
             (values
              (map #(hash-map :template_id template-id, :avu_id %) avu-ids))))))

(defn add-metadata-template-avus
  "Adds the given AVUs to the Metadata database."
  [avus]
  (korma/with-db db/metadata
    (insert :avus (values (map #(assoc % :target_type (->enum-val "data")) avus)))))

(defn update-avu
  "Updates the attribute, value, unit, and modified_on fields of the given AVU."
  [avu]
  (korma/with-db db/metadata
    (update :avus
            (set-fields (-> (select-keys avu [:attribute :value :unit])
                            (assoc :modified_on (sqlfn now))))
            (where (select-keys avu [:id])))))

(defn remove-avus
  "Removes AVUs with the given IDs from the Metadata database."
  [avu-ids]
  (korma/with-db db/metadata
    (delete :avus (where {:id [in avu-ids]}))))

(defn remove-avu
  "Removes the AVU with the given ID from the Metadata database."
  [avu-id]
  (korma/with-db db/metadata
    (delete :avus (where {:id avu-id}))))

(defn remove-data-item-template-instances
  "Removes all Metadata Template AVU associations from the given data item."
  [user-id data-id]
  (let [avu-id-select (-> (select* :avus)
                          (fields :id)
                          (where {:target_id data-id
                                  :target_type (->enum-val "data")
                                  :owner_id user-id}))]
    (korma/with-db db/metadata
    (delete :template_instances (where {:avu_id [in (subselect avu-id-select)]})))))

(defn set-template-instances
  "Associates the given AVU IDs with the given Metadata Template ID,
   removing all other Metadata Template ID associations."
  [user-id data-id template-id avu-ids]
  (korma/with-db db/metadata
    (korma/transaction
     (remove-data-item-template-instances user-id data-id)
     (add-template-instances template-id avu-ids))))


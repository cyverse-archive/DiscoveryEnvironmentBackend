(ns metadata.persistence.favorites
  (:use [korma.core :exclude [update]])
  (:require [kameleon.db :as db])
  (:import [java.util UUID]))

(defn is-favorite?
  "Indicates whether or not given target is a favorite of the given authenticated user.

   Parameters:
     user      - the authenticated user name
     target-id - the UUID of the thing being marked as a user favorite

   Returns:
     It returns true if the give target has been marked as a favorite, otherwise it returns false.
     It also returns false if the user or target doesn't exist."
  [user ^UUID target-id]
  (-> (select :favorites
        (aggregate (count :*) :cnt)
        (where {:target_id target-id :owner_id user}))
    first :cnt pos?))

(defn- base-select-favorites
  "A base selection query for all targets of a given type that have are favorites of a given
   authenticated user.

   Parameters:
     user         - the authenticated user name
     target-types - the set of types of target may belong to
                    (`analysis`|`app`|`file`|`folder`|`user`)

   Returns:
     A base selection query."
  [user target-types]
  (-> (select* :favorites)
      (fields :target_id)
      (where {:target_type [in (map db/->enum-val target-types)]
              :owner_id    user})))

(defn select-favorites-of-type
  "Selects all targets of a given type that have are favorites of a given authenticated user.

   Parameters:
     user         - the authenticated user name
     target-types - the set of types of target may belong to
                    (`analysis`|`app`|`file`|`folder`|`user`)
     target-ids   - (optional) filter the result set to IDs that match those in this set

   Returns:
     It returns a lazy sequence of favorite target UUIDs. If the user doesn't exist, the sequence
     will be empty."
  ([user target-types]
    (map :target_id (select (base-select-favorites user target-types))))
  ([user target-types target-ids]
    (map :target_id
      (-> (base-select-favorites user target-types)
          (where {:target_id [in target-ids]})
          select))))

(defn insert-favorite
  "Marks a given target as a favorite of the given authenticated user. It assumes the authenticated
   user exists and the target is of the indicated type.

   Parameters:
     user        - the authenticated user name
     target-id   - the UUID of the target
     target-type - the type of target (`analysis`|`app`|`file`|`folder`|`user`)"
  [user target-id target-type]
  (insert :favorites
    (values {:target_id   target-id
             :target_type (db/->enum-val target-type)
             :owner_id    user}))
  nil)

(defn delete-favorite
  "Unmarks a given target as a favorite of the given authenticated user.

   Parameters:
     user      - the authenticated user name
     target-id - the UUID of the target"
  [user target-id]
  (delete :favorites (where {:target_id target-id :owner_id user}))
  nil)

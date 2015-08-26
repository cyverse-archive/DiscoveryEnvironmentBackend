(ns kameleon.user-saved-searches-queries
  (:use [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids]
        [kameleon.misc-queries]
        [korma.core]))

(defn saved-searches-seq
  "Returns a seq of all of the saved searches associated with the user.
   There should only be one record for each user. Does not validate that
   user exists."
  [username]
  (select user-saved-searches
          (with users)
          (where {:users.username username})))

(defn saved-searches?
  "Returns true if the user has saved searches associated with their account.
   Does not validate that the user exists first."
  [username]
  (pos? (count (saved-searches-seq username))))

(defn saved-searches
  "Returns the saved searches associated with user. Does not validate that
   the user exists."
  [username]
  (-> (saved-searches-seq username) first :saved_searches))

(defn insert-saved-searches
  "Inserts a new record storing saved searches. Does not validate that a
   record does not already exist and does not ensure that the user exists.
   You probably want to use (save-saved-searches) instead."
  [username saved-searches-str]
  (insert user-saved-searches
          (values {:user_id        (user-id username)
                   :id             (uuid)
                   :saved_searches saved-searches-str})))

(defn save-saved-searches
  "Upserts saved searches into the database for a user. Does not validate
   that the user exists."
  [username saved-searches-str]
  (if-not (saved-searches? username)
    (insert-saved-searches username saved-searches-str)
    (do
      (update user-saved-searches
              (set-fields {:saved_searches saved-searches-str})
              (where {:user_id (user-id username)}))
      (first (select user-saved-searches
                     (where {:user_id (user-id username)}))))))

(defn delete-saved-searches
  "Hard deletes the record container a user's saved searches from the
   database."
  [username]
  (delete user-saved-searches
          (where {:user_id (user-id username)})))

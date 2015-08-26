(ns kameleon.user-prefs-queries
  (:use [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids]
        [kameleon.misc-queries]
        [korma.core]))

(defn user-prefs-seq
  "Returns all of the user preferences associated with the given username.
   There should only be one user preferences doc associated with a user.
   Does not validate that the user exists."
  [username]
  (select user-preferences
          (with users)
          (where {:users.username username})))

(defn user-prefs?
  "Returns true if the user has preferences associated with their account.
   Does not validate that the user exists."
  [username]
  (pos? (count (user-prefs-seq username))))

(defn user-prefs
  "Returns the preferences associated with the user or nil. Does not validate
   that the user exists. The return value is a string."
  [username]
  (-> (user-prefs-seq username) first :preferences))

(defn insert-user-prefs
  "Adds a new record to the database without checking to see if the user
   exists or already has preferences. Pretty dangerous, use (save-user-prefs)
   instead."
  [username prefs-str]
  (let [user-id (user-id username)]
    (insert user-preferences
            (values {:user_id user-id :preferences prefs-str :id (uuid)}))))

(defn save-user-prefs
  "Upserts user preferences into the database. Does not validate that the
   user exists."
  [username prefs-str]
  (if-not (user-prefs? username)
    (insert-user-prefs username prefs-str)
    (do
      (update user-preferences
              (set-fields {:preferences prefs-str})
              (where {:user_id (user-id username)}))
      (first (select user-preferences
                     (where {:user_id (user-id username)}))))))

(defn delete-user-prefs
  "Does a hard delete of the entire record containing a user's prefs."
  [username]
  (delete user-preferences
          (where {:user_id (user-id username)})))

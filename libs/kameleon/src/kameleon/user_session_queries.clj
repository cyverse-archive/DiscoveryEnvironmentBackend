(ns kameleon.user-session-queries
  (:use [kameleon.core]
        [kameleon.entities]
        [kameleon.uuids]
        [kameleon.misc-queries]
        [korma.core]))

(defn user-sessions-seq
  "Returns all of the sessions associated with the given username. Should
   only be a single item. Doesn't validate that the user already exists."
  [username]
  (select user-sessions
          (with users)
          (where {:users.username username})))

(defn user-session?
  "Returns a truthy value if a session is recorded for the user, a falsy
   value (should be an empty vector) otherwise. Doesn't validate that
   the user already exists."
  [username]
  (pos? (count (user-sessions-seq username))))

(defn user-session
  "Returns the session string associated with the user. Does not validate
   that the user exists."
  [username]
  (-> (user-sessions-seq username) first :session))

(defn insert-user-session
  "Inserts a new user session into the database. Does not check if a session
   record already exists for the user. Call (user-session?) first for that.
   The session-json should be a string. Doesn't validate that the user
   already exists."
  [username session-str]
  (let [user-id (user-id username)]
    (insert user-sessions
      (values {:user_id user-id :session session-str :id (uuid)}))))

(defn save-user-session
  "Safer version of insert-user-session. Will insert a new session if one
   doesn't already exist for the user, but will update the existing session
   if it does exist. Doesn't validate that the user already exists."
  [username session-str]
  (if-not (user-session? username)
    (insert-user-session username session-str)
    (do
      (update user-sessions
              (set-fields {:session session-str})
              (where {:user_id (user-id username)}))
      (first (select user-sessions
                     (where {:user_id (user-id username)}))))))

(defn delete-user-session
  "Fully evil function that deletes a user session. Deletes the entire
   record from the database, doesn't just add an empty string as the
   session. Doesn't validate that the user actually exists."
  [username]
  (when (user-session? username)
    (delete user-sessions
            (where {:user_id (user-id username)}))))

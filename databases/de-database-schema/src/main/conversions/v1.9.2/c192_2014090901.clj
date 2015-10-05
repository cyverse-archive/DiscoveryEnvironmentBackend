(ns facepalm.c192-2014090901
   (:use [korma.core]
         [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.9.2:20140909.01")

(defn- fix-user-ids
  "Fixes the IDs in references to the users table."
  [table column min-id dup-ids]
  (update table
          (set-fields {column min-id})
          (where {column [in dup-ids]})))

(defn- find-min-id
  "Finds the minimum identifier associated with a username."
  [username]
  ((comp :id first)
   (select :users
           (aggregate (min :id) :id)
           (where {:username username}))))

(defn- find-dup-ids
  "Finds the duplicate user IDs associated with a username."
  [username min-id]
  (map :id
       (select :users
               (fields :id)
               (where (and {:username username}
                           {:id [not= min-id]})))))

(defn- remove-duplicate-entries-for
  "Removes duplicate entries in the users table for a single user."
  [username]
  (let [min-id  (find-min-id username)
        dup-ids (find-dup-ids username min-id)]
    (when (seq dup-ids)
      (fix-user-ids :access_tokens :user_id min-id dup-ids)
      (fix-user-ids :authorization_requests :user_id min-id dup-ids)
      (fix-user-ids :collaborators :collaborator_id min-id dup-ids)
      (fix-user-ids :collaborators :user_id min-id dup-ids)
      (fix-user-ids :genome_reference :created_by min-id dup-ids)
      (fix-user-ids :genome_reference :last_modified_by min-id dup-ids)
      (fix-user-ids :jobs :user_id min-id dup-ids)
      (fix-user-ids :logins :user_id min-id dup-ids)
      (fix-user-ids :ratings :user_id min-id dup-ids)
      (fix-user-ids :tool_request_statuses :updater_id min-id dup-ids)
      (fix-user-ids :tool_requests :requestor_id min-id dup-ids)
      (fix-user-ids :user_preferences :user_id min-id dup-ids)
      (fix-user-ids :user_saved_searches :user_id min-id dup-ids)
      (fix-user-ids :user_sessions :user_id min-id dup-ids)
      (fix-user-ids :workspace :user_id min-id dup-ids)
      (delete :users (where {:id [in dup-ids]})))))

(defn- list-usernames
  "Lists all usernames defined in the database."
  []
  (map :username
       (select :users
               (fields :username)
               (modifier "DISTINCT"))))

(defn- remove-duplicate-users
  "Removes duplicate entries from the users table."
  []
  (println "\t* removing duplicate entries from the 'users' table")
  (dorun (map remove-duplicate-entries-for (list-usernames))))

(defn- add-username-uniqueness-constraint
  "Adds a uniqueness constraint to the username field of the users table."
  []
  (println "\t* adding a uniqueness constraint for usernames in the DE database")
  (exec-raw
   "ALTER TABLE ONLY users
    ADD CONSTRAINT username_unique
    UNIQUE (username)"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing conversion for" version)
  (remove-duplicate-users)
  (add-username-uniqueness-constraint))

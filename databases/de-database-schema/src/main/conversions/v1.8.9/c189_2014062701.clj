(ns facepalm.c189-2014062701
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.9:20140627.01")

(defn- add-authorization-requests-foreign-key
  "Adds foreign key constraints to the authorization_requests table."
  []
  (println "\t* adding the user_id constraint to the authorization_requests table")
  (exec-raw
   "ALTER TABLE ONLY authorization_requests
    ADD CONSTRAINT authorization_requests_user_id_fkey
    FOREIGN KEY (user_id)
    REFERENCES users(id)"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (add-authorization-requests-foreign-key))

(ns facepalm.c188-2014052201
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.8:20140522.01")

(defn- add-authorization-requests-table
  []
  (println "\t* adding the authorization_requests table")
  (exec-raw
   "CREATE TABLE authorization_requests (
    id UUID NOT NULL,
    user_id BIGINT UNIQUE NOT NULL,
    state_info TEXT NOT NULL,
    PRIMARY KEY (id))"))

(defn convert
  "Performs the conversion for database version 1.8.8:20140522.01"
  []
  (println "Performing the conversion for" version)
  (add-authorization-requests-table))

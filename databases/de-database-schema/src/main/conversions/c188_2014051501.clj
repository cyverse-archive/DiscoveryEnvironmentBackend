(ns facepalm.c188-2014051501
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.8:20140515.01")

(defn- add-access-tokens-table
  []
  (println "\t* adding the access_tokens table")
  (exec-raw
   "CREATE TABLE access_tokens (
    webapp VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP,
    refresh_token VARCHAR(128),
    PRIMARY KEY (webapp, user_id))"))

(defn convert
  "Performs the conversion for database version 1.8.8:20140515.01"
  []
  (println "Performing the conversion for" version)
  (add-access-tokens-table))

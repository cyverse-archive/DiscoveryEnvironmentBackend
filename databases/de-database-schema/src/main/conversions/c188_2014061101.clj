(ns facepalm.c188-2014061101
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.8:20140611.01")

(defn- drop-access-tokens-table
  []
  (println "\t* dropping the access_tokens table")
  (exec-raw "DROP TABLE access_tokens"))

(defn- regenerate-access-tokens-table
  []
  (println "\t* regenerating the access_tokens table")
  (exec-raw
   "CREATE TABLE access_tokens (
    webapp VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    token BYTEA NOT NULL,
    expires_at TIMESTAMP,
    refresh_token BYTEA,
    PRIMARY KEY (webapp, user_id))"))

(defn convert
  "Performs the conversion for database version 1.8.8:20140611.01"
  []
  (println "Performing the conversion for" version)
  (drop-access-tokens-table)
  (regenerate-access-tokens-table))

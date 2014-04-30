(ns facepalm.c187-2014042101
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.6:20140421.01")

(defn- add-user-preferences-table
  []
  (println "\t* adding the user_preferences table")
  (exec-raw
   "CREATE TABLE user_preferences (
        id UUID UNIQUE NOT NULL,
        user_id BIGINT UNIQUE NOT NULL REFERENCES users(id),
        preferences TEXT NOT NULL,
        PRIMARY KEY (id)
    )")
  (exec-raw
   "CREATE INDEX user_preferences_id
    ON user_preferences(id)")
  (exec-raw
   "CREATE INDEX user_preferences_user_id
    ON user_preferences(user_id)"))

(defn- add-sessions-table
  []
  (println "\t* adding the user_sessions table")
  (exec-raw
   "CREATE TABLE user_sessions (
        id UUID UNIQUE NOT NULL,
        user_id BIGINT UNIQUE NOT NULL REFERENCES users(id),
        session TEXT NOT NULL,
        PRIMARY KEY (id)
    )")
  (exec-raw
   "CREATE INDEX user_sessions_id
    ON user_sessions(id)")
  (exec-raw
   "CREATE INDEX user_session_user_id
    ON user_sessions(user_id)"))

(defn- add-saved-searches-table
  []
  (println "\t* adding the user_saved_searches table")
  (exec-raw
   "CREATE TABLE user_saved_searches (
        id UUID UNIQUE NOT NULL,
        user_id BIGINT UNIQUE NOT NULL REFERENCES users(id),
        saved_searches TEXT NOT NULL,
        PRIMARY KEY (id)
    )")
  (exec-raw
   "CREATE INDEX user_saved_searches_id
    ON user_saved_searches(id)")
  (exec-raw
   "CREATE INDEX user_saved_searches_user_id
    ON user_saved_searches(user_id)"))

(defn- add-tree-urls-table
  []
  (println "\t* adding the tree_urls table")
  (exec-raw
   "CREATE TABLE tree_urls (
        id UUID UNIQUE NOT NULL,
        tree_urls TEXT NOT NULL,
        PRIMARY KEY (id)
    )")
  (exec-raw
   "CREATE INDEX tree_urls_id
    ON tree_urls(id)"))

(defn convert
  "Performs the conversion for database version 1.8.4:20140421.01."
  []
  (println "Performing conversion for " version)
  (add-user-preferences-table)
  (add-sessions-table)
  (add-saved-searches-table)
  (add-tree-urls-table))

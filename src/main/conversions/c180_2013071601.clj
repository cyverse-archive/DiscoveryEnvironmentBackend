(ns facepalm.c180-2013071601
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130716.01")

(defn- define-logins-table
  "Defines the table used to record logins for each user."
  []
  (println "\t* defining the logins table.")
  (exec-raw "CREATE TABLE logins (
    user_id bigint REFERENCES users(id),
    ip_address varchar(15) NOT NULL,
    login_time timestamp NOT NULL DEFAULT now(),
    logout_time timestamp)"))

(defn convert
  "Performs the conversion for database version 1.8.0:20130716.01."
  []
  (println "Performing conversion for" version)
  (define-logins-table))

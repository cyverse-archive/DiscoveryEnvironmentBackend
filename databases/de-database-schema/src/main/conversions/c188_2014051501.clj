(ns facepalm.c188-2014051501
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.8:20140515.01")

(defn- add-integrated-webapps-table
  []
  (println "\t* adding the integrated_webapps table")
  (exec-raw
   "CREATE TABLE integrated_webapps (
    id UUID NOT NULL,
    name VARCHAR(64) NOT NULL,
    PRIMARY KEY (id))")
  (exec-raw
   "ALTER TABLE ONLY integrated_webapps
    ADD CONSTRAINT integrated_webapps_unique_name
    UNIQUE (name)"))

(def ^:private integrated-webapps
  [["5AF0CB5F-840E-4A50-A0BD-6A61F71A5746", "Agave API"]])

(defn- prepare-integrated-webapps
  []
  (letfn [(format-webapp [[id name]] {:id (UUID/fromString id) :name name})]
    (into {} (map format-webapp integrated-webapps))))

(defn- populate-integrated-webapps-table
  []
  (println "\t* populating the integrated_webapps table")
  (insert :integrated_webapps
          (values (prepare-integrated-webapps))))

(defn- add-authorization-tokens-table
  []
  (println "\t* adding the authorization_tokens table")
  (exec-raw
   "CREATE TABLE authorization_tokens (
    id UUID NOT NULL,
    webapp_id UUID NOT NULL REFERENCES integrated_webapps(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    token VARCHAR(128) NOT NULL,
    PRIMARY KEY (id))")
  (exec-raw
   "ALTER TABLE ONLY authorization_tokens
    ADD CONSTRAINT authorization_tokens_unique_webapp_id_and_user_id
    UNIQUE (webapp_id, user_id)"))

(defn convert
  "Performs the conversion for database version 1.8.8:20140515.01"
  []
  (add-integrated-webapps-table)
  (populate-integrated-webapps-table)
  (add-authorization-tokens-table))

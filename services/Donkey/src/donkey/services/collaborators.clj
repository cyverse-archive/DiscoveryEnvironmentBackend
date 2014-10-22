(ns donkey.services.collaborators
  (:use [donkey.auth.user-attributes :only [current-user]]
        [donkey.util.config :only [uid-domain]]
        [donkey.util.service :only [success-response decode-stream]]
        [korma.db :only [with-db]])
  (:require [clojure.string :as string]
            [donkey.clients.user-info :as user-info]
            [donkey.util.db :as db]
            [kameleon.queries :as queries]))

(defn- add-domain
  "Adds the username domain to a username."
  [username]
  (str username "@" (uid-domain)))

(defn- remove-domain
  "Removes the username domain from a username."
  [username]
  (string/replace username #"@.*" ""))

(defn- add-user-details
  "Adds user details to the results from a request to obtain a list of collaborators."
  [users]
  (map user-info/get-user-details (filter #(not (string/blank? %)) users)))

(defn get-collaborators
  "Gets the list of collaborators for the current user and retrieves detailed information from
   Trellis."
  [req]
  (let [collaborators (with-db db/de
                        (queries/get-collaborators (:username current-user)))
        collaborators (map remove-domain collaborators)]
    (success-response {:users (add-user-details collaborators)})))

(defn- extract-usernames
  "Extracts the usernames from the request body for the services to add and
   remove collaborators."
  [{:keys [users]}]
  (map :username users))

(defn add-collaborators
  "Adds collaborators for the current user."
  [req]
  (let [collaborators (extract-usernames (decode-stream (:body req)))]
    (with-db db/de
      (queries/add-collaborators (:username current-user) (map add-domain collaborators)))
    (success-response)))

(defn remove-collaborators
  "Removes collaborators for the current user."
  [req]
  (let [collaborators (extract-usernames (decode-stream (:body req)))]
    (with-db db/de
      (queries/remove-collaborators (:username current-user) (map add-domain collaborators)))
    (success-response)))

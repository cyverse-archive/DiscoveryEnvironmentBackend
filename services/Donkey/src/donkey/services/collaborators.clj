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

(defn- extract-usernames
  "Extracts the usernames from the request body for the services to add and
   remove collaborators."
  [{:keys [users]}]
  (map :username users))

(defn remove-collaborators
  "Removes collaborators for the current user."
  [req]
  (let [collaborators (extract-usernames (decode-stream (:body req)))]
    (with-db db/de
      (queries/remove-collaborators (:username current-user) (map add-domain collaborators)))
    (success-response)))

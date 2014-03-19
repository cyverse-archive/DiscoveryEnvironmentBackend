(ns metadactyl.collaborators
  (:use [korma.db :only [transaction]]
        [metadactyl.util.config :only [uid-domain]]
        [metadactyl.util.service :only [success-response]])
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]
            [kameleon.queries :as queries]))

(defn- add-domain
  "Adds the username domain to a username."
  [username]
  (str username "@" (uid-domain)))

(defn- remove-domain
  "Removes the username domain from a username."
  [username]
  (string/replace username #"@.*" ""))

(defn get-collaborators
  "Gets the list of collaborators for the current user."
  [{:keys [user]}]
  (let [collaborators (queries/get-collaborators (add-domain user))]
    (success-response {:users (map remove-domain collaborators)})))

(defn add-collaborators
  "Adds collaborators for the current user."
  [{:keys [user]} body]
  (transaction
   (let [collaborators (:users (cheshire/decode body true))]
     (queries/add-collaborators (add-domain user) (map add-domain collaborators))
     (success-response))))

(defn remove-collaborators
  "Removes collaborators for the current user."
  [{:keys [user]} body]
  (transaction
   (let [collaborators (:users (cheshire/decode body true))]
     (queries/remove-collaborators (add-domain user)
                                   (map add-domain collaborators))
     (success-response))))

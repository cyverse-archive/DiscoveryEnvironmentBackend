(ns metadactyl.service.collaborators
  (:use [metadactyl.util.conversions :only [remove-nil-vals]])
  (:require [clojure.string :as string]
            [kameleon.queries :as queries]
            [metadactyl.clients.iplant-groups :as ipg]
            [metadactyl.util.config :as config]))

(defn- remove-domain
  "Removes the username domain from a username."
  [username]
  (string/replace username #"@.*" ""))

(defn- add-domain
  "Adds the username domain to a username."
  [username]
  (str username "@" (config/uid-domain)))

(defn- format-like-trellis
  "Reformat an iplant-groups response to look like a trellis response."
  [response]
  {:username (:id response)
   :firstname (:first_name response)
   :lastname (:last_name response)
   :email (:email response)
   :institution (:institution response)})

(defn get-collaborators
  "Gets the list of collaborators for the current user and retrieves detailed information from
   Trellis."
  [{:keys [username]}]
  (->> (queries/get-collaborators username)
       (map remove-domain)
       (remove string/blank?)
       (map (partial ipg/lookup-subject (remove-domain username)))
       (map remove-nil-vals)
       (map format-like-trellis)
       (hash-map :users)))

(defn add-collaborators
  "Adds users to the authenticated user's list of collaborators."
  [{:keys [username]} {:keys [users]}]
  (queries/add-collaborators username (map (comp add-domain :username) users))
  nil)

(defn remove-collaborators
  "Removes users from the authenticated user's list of collaborators."
  [{:keys [username]} {:keys [users]}]
  (queries/remove-collaborators username (map (comp add-domain :username) users))
  nil)

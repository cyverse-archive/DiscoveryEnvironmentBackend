(ns metadactyl.service.collaborators
  (:use [metadactyl.util.conversions :only [remove-nil-vals]])
  (:require [clojure.string :as string]
            [kameleon.queries :as queries]
            [metadactyl.clients.trellis :as trellis]
            [metadactyl.util.config :as config]))

(defn- remove-domain
  "Removes the username domain from a username."
  [username]
  (string/replace username #"@.*" ""))

(defn- add-domain
  "Adds the username domain to a username."
  [username]
  (str username "@" (config/uid-domain)))

(defn get-collaborators
  "Gets the list of collaborators for the current user and retrieves detailed information from
   Trellis."
  [{:keys [username]}]
  (->> (queries/get-collaborators username)
       (map remove-domain)
       (remove string/blank?)
       (map trellis/get-user-details)
       (map remove-nil-vals)
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

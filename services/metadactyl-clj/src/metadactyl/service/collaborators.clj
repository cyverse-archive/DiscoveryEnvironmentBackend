(ns metadactyl.service.collaborators
  (:use [metadactyl.util.conversions :only [remove-nil-vals]])
  (:require [clojure.string :as string]
            [kameleon.queries :as queries]
            [metadactyl.clients.trellis :as trellis]))

(defn- remove-domain
  "Removes the username domain from a username."
  [username]
  (string/replace username #"@.*" ""))

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

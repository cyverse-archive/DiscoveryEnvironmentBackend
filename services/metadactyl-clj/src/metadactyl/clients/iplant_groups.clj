(ns metadactyl.clients.iplant-groups
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [metadactyl.util.config :as config]))

(defn- lookup-subject-url
  [short-username]
  (str (curl/url (config/ipg-base) "subjects" short-username)))

(defn lookup-subject
  "Uses iplant-groups's subject lookup by ID endpoint to retrieve user details."
  [user short-username]
  (-> (http/get (lookup-subject-url short-username) {:query-params {:user user} :as :json})
      (:body)))

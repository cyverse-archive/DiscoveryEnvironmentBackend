(ns donkey.services.user-info
  (:use [clojure.string :only [split blank?]]
        [clojure-commons.validators]
        [donkey.util.config]
        [donkey.util.service :only [success-response]]
        [byte-streams]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [donkey.clients.iplant-groups :as ipg]
            [donkey.auth.user-attributes :as user]
            [clojure.tools.logging :as log]))

(defn user-search
  "Performs user searches by username, name and e-mail address and returns the
   merged results."
  [search-string]
     (let [results (ipg/search-subjects (:shortUsername user/current-user) search-string)
           users (map ipg/format-like-trellis (:subjects results))]
       (success-response {:users users :truncated false})))

(defn- add-user-info
  "Adds the information for a single user to a user-info lookup result."
  [result [username user-info]]
  (if (nil? user-info)
    result
    (assoc result username (ipg/format-like-trellis user-info))))

(defn- get-user-info
  "Gets the information for a single user, returning a vector in which the first
   element is the username and the second element is either the user info or nil
   if the user doesn't exist."
  [username]
  (->> (ipg/lookup-subject (:shortUsername user/current-user) username)
       (vector username)))

(defn user-info
  "Performs a user search for one or more usernames, returning a response whose
   body consists of a JSON object indexed by username."
  [usernames]
  (let [body (reduce add-user-info {} (map get-user-info usernames))]
    {:status       200
     :body         (cheshire/encode body)
     :content-type :json}))

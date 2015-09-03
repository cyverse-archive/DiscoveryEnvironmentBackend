(ns donkey.clients.iplant-groups
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [donkey.util.config :as config]))

(defn format-like-trellis
  "Reformat an iplant-groups response to look like a trellis response."
  [response]
  {:username (:id response)
   :firstname (:first_name response)
   :lastname (:last_name response)
   :email (:email response)
   :institution (:institution response)})

(defn- empty-user-info
  "Returns an empty user-info record for the given username."
  [username]
  {:email     ""
   :firstname ""
   :id        username
   :lastname  ""})

(defn- lookup-subject-url
  [short-username]
  (str (curl/url (config/ipg-base) "subjects" short-username)))

(defn lookup-subject
  "Uses iplant-groups's subject lookup by ID endpoint to retrieve user details."
  [user short-username]
  (try
    (if-let [user-info (-> (http/get (lookup-subject-url short-username) {:query-params {:user user} :as :json})
                           (:body))]
      user-info
      (do (log/warn (str "no user info found for username '" short-username "'"))
          nil))
    (catch Exception e
      (log/error e (str "username lookup for '" short-username "' failed"))
      nil)))

(defn lookup-subject-add-empty
  "Uses iplant-groups's subject lookup by ID endpoint to retrieve user details, returning an empty user info block if nothing is found."
  [user short-username]
  (if-let [user-info (lookup-subject user short-username)]
    user-info
    (empty-user-info short-username)))

(defn search-subjects
  "Uses iplant-groups's subject search endpoint to retrieve user details."
  [user search]
  (let [res (http/get (str (curl/url (config/ipg-base) "subjects"))
                      ;; Adding wildcards matches previous (trellis) search behavior
                      {:query-params {:user user :search (str "*" search "*")}
                       :as           :json})
        status (:status res)]
    (when-not (#{200 404} status)
      (throw (Exception. (str "iplant-groups service returned status " status))))
    {:subjects (:subjects (:body res))}))

(ns metadactyl.clients.trellis
  (:require [cemerick.url :as curl]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [metadactyl.util.config :as config]))

(defn- user-search-url
  "Builds a URL that can be used to perform a specific type of user search."
  [type search-string]
  (str (curl/url (config/userinfo-base) "users" type search-string)))

(defn- extract-range
  "Extracts a range of results from a list of results."
  [start end results]
  (let [max-count (- end start)]
    (if (> (count results) max-count)
      (take max-count (drop start results))
      results)))

(defn search
  "Performs a user search and returns the results as a vector of maps."
  [type search-string start end]
  (let [res (http/get (user-search-url type search-string)
                      {:as               :json
                       :insecure?        true
                       :throw-exceptions false
                       :headers          {"range" (str "records=" start "-" end)}
                       :basic-auth       [(config/userinfo-key) (config/userinfo-secret)]})
        status (:status res)]
    (when-not (#{200 206 404} status)
      (throw (Exception. (str "user info service returned status " status))))
    {:users     (extract-range start end (:users (:body res)))
     :truncated (= status 206)}))

(defn- empty-user-info
  "Returns an empty user-info record for the given username."
  [username]
  {:email     ""
   :firstname ""
   :id        "-1"
   :lastname  ""
   :username  username})

(defn- find-user-info
  [username]
  (->> (search "username" username 0 100)
       (:users)
       (filter (comp (partial = username) :username))
       (first)))

(defn get-user-details
  "Performs a user search for a single username."
  [username]
  (try
    (if-let [user-info (find-user-info username)]
      user-info
      (do (log/warn (str "no user info found for username '" username "'"))
          (empty-user-info username)))
    (catch Exception e
      (log/error e (str "username search for '" username "' failed"))
      (empty-user-info username))))

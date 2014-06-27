(ns donkey.services.user-info
  (:use [cemerick.url :only [url]]
        [clojure.string :only [split blank?]]
        [clojure-commons.validators]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.validators]
        [donkey.util.config]
        [byte-streams]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.kv :as kv]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clojure.tools.logging :as log]
            [ring.util.response :as rsp]))

(defn- user-search-url
  "Builds a URL that can be used to perform a specific type of user search."
  [type search-string]
  (str (url (userinfo-base-url) "users" type search-string)))

(defn- extract-range
  "Extracts a range of results from a list of results."
  [start end results]
  (let [max-count (- end start)]
    (if (> (count results) max-count)
      (take max-count (drop start results))
      results)))

(defn- search
  "Performs a user search and returns the results as a vector of maps."
  [type search-string start end]
  (let [res (client/get (user-search-url type search-string)
                        {:insecure? true
                         :throw-exceptions false
                         :headers {"range" (str "records=" start "-" end)}
                         :basic-auth [(userinfo-key) (userinfo-secret)]})
        status (:status res)]
    (when-not (#{200 206 404} status)
      (throw (Exception. (str "user info service returned status " status))))
    {:users (extract-range start end (:users (cheshire/decode (:body res) true)))
     :truncated (= status 206)}))

(def
  ^{:private true
    :doc "The list of functions to use in a generalized search."}
  search-fns [(partial search "name") (partial search "email")])

(defn- remove-duplicates
  "Removes duplicate user records from the merged search results.  We use
   (map val ...) here rather than (vals) because (vals) returns nil if the
   map is empty."
  [results]
  (map val (into {} (map #(vector (:id %) %) results))))

(defn- to-int
  "Converts a string to an integer, throwing an IllegalArgumentException if
   the number can't be parsed.  This function is intended to be used from
   within parse-range."
  [string]
  (try
    (Integer/parseInt string)
    (catch NumberFormatException e
      (throw (IllegalArgumentException.
              (str "invalid number format in Range header: " string) e)))))

(defn- parse-range
  "Parses the value of a range header in the request.  We expect the header
   value to be in the format, records=<first>-<last>.  For example, to get
   records 0 through 50, the header value should be records=0-50."
  [value]
  (if (nil? value)
    [0 (default-user-search-result-limit)]
    (let [[units begin-str end-str] (split value #"[=-]")
          [begin end] (map to-int [begin-str end-str])]
      (if (or (not= "records" units) (neg? begin) (neg? end) (>= begin end))
        (throw (IllegalArgumentException.
                "invalid Range header value: should be records=0-50")))
      [begin end])))

(defn user-search
  "Performs user searches by username, name and e-mail address and returns the
   merged results."
  ([{:keys [search]} {:strs [range]}]
     (validate-field "search" search (comp not blank?))
     (apply user-search search (parse-range range)))
  ([search-string start end]
     (let [results (map #(% search-string start end) search-fns)
           users (remove-duplicates (mapcat :users results))
           truncated (if (some :truncated results) true false)]
       (cheshire/encode {:users users :truncated truncated}))))

(defn- empty-user-info
  "Returns an empty user-info record for the given username."
  [username]
  {:email     ""
   :firstname ""
   :id        "-1"
   :lastname  ""
   :username  username})

(defn get-user-details
  "Performs a user search for a single username."
  [username]
  (try
    (let [info (first (filter #(= (:username %) username)
                              (:users (search "username" username 0 100))))]
      (if (nil? info)
        (do
          (log/warn (str "no user info found for username '" username "'"))
          (empty-user-info username))
        info))
    (catch Exception e
      (log/error e (str "username search for '" username "' failed"))
      (empty-user-info username))))

(defn- add-user-info
  "Adds the information for a single user to a user-info lookup result."
  [result [username user-info]]
  (if (nil? user-info)
    result
    (assoc result username user-info)))

(defn- get-user-info
  "Gets the information for a single user, returning a vector in which the first
   element is the username and the second element is either the user info or nil
   if the user doesn't exist."
  [username]
  (->> (search "username" username 0 100)
       (:users)
       (filter #(= (:username %) username))
       (first)
       (vector username)))

(defn user-info
  "Performs a user search for one or more usernames, returning a response whose
   body consists of a JSON object indexed by username."
  [usernames]
  (let [body (reduce add-user-info {} (map get-user-info usernames))]
    {:status       200
     :body         (cheshire/encode body)
     :content-type :json}))

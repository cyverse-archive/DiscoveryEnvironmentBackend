(ns donkey.services.user-info
  (:use [clojure.string :only [split blank?]]
        [clojure-commons.validators]
        [donkey.util.config]
        [donkey.util.service :only [success-response]]
        [byte-streams]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.kv :as kv]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.clients.user-info :as uc]
            [clojure.tools.logging :as log]
            [ring.util.response :as rsp]))

(def
  ^{:private true
    :doc "The list of functions to use in a generalized search."}
  search-fns [(partial uc/search "name") (partial uc/search "email")])

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
       (success-response {:users users :truncated truncated}))))

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
  (->> (uc/search "username" username 0 100)
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

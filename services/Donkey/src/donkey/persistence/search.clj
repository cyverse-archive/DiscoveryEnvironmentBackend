(ns donkey.persistence.search
  "provides the functions that interact directly with elasticsearch"
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.document :as doc]
            [clojurewerkz.elastisch.rest.response :as resp]
            [slingshot.slingshot :refer [try+ throw+]]
            [donkey.util.config :as cfg])
  (:import [java.net ConnectException]
           [clojure.lang PersistentArrayMap PersistentVector]))


(defn- format-response
  [resp]
  (letfn [(format-match [match] {:score    (:_score match)
                                 :type     (:_type match)
                                 :document (:_source match)})]
    {:total (or (resp/total-hits resp) 0) :matches (map format-match (resp/hits-from resp))}))


(defn ^PersistentArrayMap search-data
  "Sends a search query to the elasticsearch data index.

   Parameters:
     types - the list of mapping types to search. Valid types are :file for files and :folder for
             folders.
     query - the elastisch prepared search query
     sort  - the elastisch prepared sort criteria
     from  - the number of search results to skip before first returned result
     size  - the maximum number of search results to return

   Returns:
     It returns the elastisch formatted search results.

   Throws:
     :invalid-configuration - This is thrown if there is a problem with elasticsearch
     :invalid-query - This is thrown if the query string is invalid."
  [^PersistentVector   types
   ^PersistentArrayMap query
   ^PersistentArrayMap sort
   ^Integer            from
   ^Integer            size]
  (try+
    (let [resp (doc/search (es/connect (cfg/es-url)) "data" (map name types)
                 :query        query
                 :from         from
                 :size         size
                 :sort         sort
                 :track_scores true)]
      (format-response resp))
    (catch ConnectException _
      (throw+ {:type :invalid-configuration :reason "cannot connect to elasticsearch"}))
    (catch [:status 404] {:keys []}
      (throw+ {:type :invalid-configuration :reason "elasticsearch has not been initialized"}))
    (catch [:status 400] {:keys []}
      (throw+ {:type :invalid-query}))))

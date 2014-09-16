(ns donkey.persistence.search
  "provides the functions that interact directly with elasticsearch"
  (:require [clojurewerkz.elastisch.query :as query]
            [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.document :as doc]
            [clojurewerkz.elastisch.rest.response :as resp]
            [slingshot.slingshot :refer [try+ throw+]]
            [donkey.util.config :as cfg])
  (:import [java.net ConnectException]
           [clojure.lang IPersistentMap ISeq]))


(defn- tags-access-filter
  [tags memberships]
  (letfn [(tag-filter [tag] (query/term :id {:type "tag"
                                             :id   tag
                                             :path "targets.id"}))]
    (query/bool :must   (query/nested :path   "userPermissions"
                                      :filter (query/term "userPermissions.user" memberships))
                :should (map tag-filter tags))))


(defn ^IPersistentMap mk-data-query
  "Builds a search query for finding all of the data entries that match a given query that are
   visible to at least one of the provided user or group names. If tags are provided, the results
   will have at least one of the tags.

   Parameters:
     query       - the elastisch formated query to execute.
     tags        - a list of tags that may be matched.
     memberships - the set of iRODS zone qualified usernames used to filter the query.

   Returns:
     It returns the elastisch formatted query filtered for tags and user access."
  [^IPersistentMap query ^ISeq tags ^ISeq memberships]
  (query/filtered :query query :filter (tags-access-filter tags memberships)))


(defn ^IPersistentMap mk-data-tags-filter
  "Builds a search filter for finding all of the data entries that are visible to at least one of
   the provided user or group names. If tags are provided, the results will have at least one of the
   tags.

   Parameters:
     tags        - a list of tags that may be matched.
     memberships - the set of iRODS zone qualified usernames used to filter the query.

   Returns:
     It returns the elastisch formatted filter for tags and user access."
  [^ISeq tags ^ISeq memberships]
  (query/filtered :filter (tags-access-filter tags memberships)))


(defn- format-response
  [resp]
  (letfn [(format-match [match] {:score    (:_score match)
                                 :type     (:_type match)
                                 :document (:_source match)})]
    {:total (or (resp/total-hits resp) 0) :matches (map format-match (resp/hits-from resp))}))


(defn ^IPersistentMap search-data
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
  [^ISeq types ^IPersistentMap query ^IPersistentMap sort ^Integer from ^Integer size]
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

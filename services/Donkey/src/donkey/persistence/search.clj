(ns donkey.persistence.search
  "provides the functions that interact directly with elasticsearch"
  (:require [clojurewerkz.elastisch.query :as query]
            [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.document :as doc]
            [clojurewerkz.elastisch.rest.response :as resp]
            [slingshot.slingshot :refer [try+ throw+]]
            [donkey.util.config :as cfg]
            [clojure-commons.exception :as cx])
  (:import [java.net ConnectException]
           [java.util UUID]
           [clojure.lang IPersistentMap ISeq]))


(def ^:private es-uninitialized {:type   ::cx/invalid-cfg
                                 :error "elasticsearch has not been initialized"})


(defn- connect
  []
  (try+
    (es/connect (cfg/es-url))
    (catch ConnectException _
      (throw+ {:type ::cx/invalid-cfg
               :error "cannot connect to elasticsearch"}))))


(defn index-tag
  "Inserts a tag into the search index.

   Parameters:
     tag - the tag document to insert.

   Throws:
     ::cx/invalid-cfg - This is thrown if there is a problem with elasticsearch"
  [^IPersistentMap tag]
  (try+
    (doc/create (connect) "data" "tag" tag :id (:id tag))
    (catch [:status 404] {:keys []}
      (throw+ es-uninitialized))))


(defn update-tag
  "Updates a tag's label, description, modification date.

   Parameters:
     tag-id - the id of the tag to update
     updates - a map containing the updated values.

   Throws:
     ::cx/invalid-cfg - This is thrown if there is a problem with elasticsearch"
  [^UUID tag-id ^IPersistentMap updates]
  (try+
    (let [script "ctx._source.value = value;
                  ctx._source.description = description;
                  ctx._source.dateModified = dateModified"]
      (doc/update-with-script (connect) "data" "tag" (str tag-id) script updates))
    (catch [:status 404] {:keys []}
      (throw+ es-uninitialized))))


(defn update-tag-targets
  "Updates a tag's target list.

   Parameters:
     tag-id - the id of the tag to update
     targets - a list of the current targets docs.

   Throws:
     ::cx/invalid-cfg - This is thrown if there is a problem with elasticsearch"
  [^UUID tag-id ^ISeq targets]
  (try+
    (let [script "ctx._source.targets = targets"
          update {:targets (map #(assoc % :type (str (:type %))) targets)}]
      (doc/update-with-script (connect) "data" "tag" (str tag-id) script update))
    (catch [:status 404] {:keys []}
      (throw+ es-uninitialized))))


(defn remove-tag
  "Removes a tag from the search index.

   Parameters:
     tag-id - the id of the tag document to remove.

   Throws:
     ::cx/invalid-cfg - This is thrown if there is a problem with elasticsearch"
  [^UUID tag-id]
  (try+
    (doc/delete (connect) "data" "tag" (str tag-id))
    (catch [:status 404] {:keys []}
      (throw+ es-uninitialized))))


(defn ^ISeq filter-user-tags
  "Filters a given set of tags for those created by a given user.

   Parameters:
     user    - the iRODS zone qualified username of the user who must have created the tags
     tag-ids - the ids of the tags to filter.

   Returns:
     It returns the subset of the given tags that are owned by the given user."
  [^String user ^ISeq tag-ids]
  (try+
    (let [query (query/filtered :query (query/term :id tag-ids) :filter (query/term :creator user))
          hits  (resp/hits-from (doc/search (connect) "data" "tag" :query query :_source false))]
      (map :_id hits))
    (catch [:status 404] {:keys []}
      (throw+ es-uninitialized))))


(defn- tags-access-filter
  [tags memberships]
  (letfn [(tag-filter [tag] (query/term :id {:type  "tag"
                                             :id    tag
                                             :path  "targets.id"
                                             :cache false}))]
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
     ::cx/invalid-cfg - This is thrown if there is a problem with elasticsearch
     :invalid-query - This is thrown if the query string is invalid."
  [^ISeq types ^IPersistentMap query ^IPersistentMap sort ^Integer from ^Integer size]
  (try+
    (let [resp (doc/search (connect) "data" (map name types)
                 :query        query
                 :from         from
                 :size         size
                 :sort         sort
                 :track_scores true)]
      (format-response resp))
    (catch [:status 404] {:keys []}
      (throw+ es-uninitialized))
    (catch [:status 400] {:keys []}
      (throw+ {:type :invalid-query}))))

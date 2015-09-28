(ns donkey.services.search
  "provides the functions that forward search requests to Elastic Search"
  (:use [clojure-commons.error-codes :only [invalid-arg-response]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [clj-time.core :as t]
            [clj-time.local :as l]
            [donkey.persistence.search :as search]
            [donkey.clients.data-info :as data]
            [donkey.util.config :as cfg]
            [donkey.util.service :as svc]
            [donkey.util.validators :as valid])
  (:import [java.net ConnectException]
           [java.util UUID]
           [clojure.lang IPersistentMap]))


(defn- send-request
  "Sends the search request to Elastic Search.

   Throws:
     ::cx/invalid-cfg - This is thrown if there is a problem with elasticsearch
     :invalid-query - This is thrown if the query string is invalid."
  [type query from size sort]
  (log/debug "sending query" query)
  (let [types (if (= type :any) [:file :folder] [type])]
    (search/search-data types query sort from size)))


(defn- format-entity
  "Formats an entity for the response"
  [es-form memberships]
  (let [filter-members (fn [perm-map] (select-keys perm-map memberships))
        max-perm       (fn [perms] (cond
                                     (contains? perms :own)   :own
                                     (contains? perms :write) :write
                                     (contains? perms :read)  :read))
        agg-perm       (->> (:userPermissions es-form)
                         (map #(vector (:user %) (:permission %)))
                         flatten
                         (apply hash-map)
                         filter-members
                         vals
                         (map keyword)
                         set
                         max-perm)]
    (assoc es-form :permission agg-perm)))


(defn- extract-result
  "Extracts the result of the Donkey search services from the results returned to us by
   ElasticSearch."
  [resp offset memberships]
  (letfn [(format-match [match] {:score  (:score match)
                                 :type   (:type match)
                                 :entity (format-entity (:document match) memberships)})]
    (assoc resp :offset offset :matches (map format-match (:matches resp)))))


(defn- mk-data-query
  "Builds the full query for elasticsearch."
  [query tags memberships]
    (if query
      (search/mk-data-query query tags memberships)
      (search/mk-data-tags-filter tags memberships)))


(defn- mk-sort
  "Builds a sort request."
  [sort]
  (let [field (case (ffirst sort)
                :entity (second (first sort))
                :score  :_score
                :type   :_type)]
    [{field {:order           (name (second sort))
             :missing         "_last"
             :ignore_unmapped true}}]))


(defn- extract-direction
  "Extracts the direction portion of the sort parameter"
  [dir-str default]
  (if (or (nil? dir-str) (= "" dir-str))
    default
    (case dir-str
      "asc"  :asc
      "desc" :desc
             (throw+ {:type   :invalid-argument
                      :reason "direction must be 'asc' or 'desc'"
                      :arg    :sort
                      :val    dir-str}))))


(defn- extract-field
  "Extracts the field portion of the sort parameter."
  [field-str]
  (case (string/lower-case field-str)
    "score"                             [:score]
    "type"                              [:type]
    "entity.creator"                    [:entity :creator]
    "entity.datecreated"                [:entity :dateCreated]
    "entity.datemodified"               [:entity :dateModified]
    "entity.filesize"                   [:entity :fileSize]
    "entity.filetype"                   [:entity :fileType]
    "entity.id"                         [:entity :id]
    "entity.label"                      [:entity :label]
    "entity.path"                       [:entity :path]
    "entity.metadata.attribute"         [:entity :metadata.attribute]
    "entity.metadata.unit"              [:entity :metadata.unit]
    "entity.metadata.value"             [:entity :metadta.value]
    "entity.userpermissions.permission" [:entity :userPermissions.permission]
    "entity.userpermissions.user"       [:entity :userPermissions.user]
                                        (throw+ {:type   :invalid-argument
                                                 :reason "invalid match record field"
                                                 :arg    :sort
                                                 :val    field-str})))


(defn- extract-sort
  "Extracts the sort field and direction from the URL parameters"
  [params default]
  (if-let [sort-val (:sort params)]
    (let [[field-str dir-str] (string/split sort-val #":")]
      [(extract-field field-str) (extract-direction dir-str (last default))])
    default))


(defn- extract-type
  "Extracts the entity type from the URL parameters

   Throws:
     :invalid-argument - This is thrown if the extracted type isn't valid."
  [params]
  (try+
    (valid/resolve-entity-type (:type params))
    (catch [:type :invalid-argument] exn
      (throw+ (assoc exn :arg "type")))))


(defn- extract-uint
  "Extracts a non-negative integer from the URL parameters

   Throws:
     :invalid-argument - This is thrown if the parameter value isn't a non-negative integer."
  [params name-key default]
  (letfn [(mk-exception [val] {:type   :invalid-argument
                               :reason "must be a non-negative integer"
                               :arg    name-key
                               :val    val})]
    (if-let [val-str (name-key params)]
      (try+
        (let [val (Integer. val-str)]
          (when (neg? val)
            (throw+ (mk-exception val)))
          val)
        (catch NumberFormatException _
          (throw+ (mk-exception val-str))))
      default)))


(defn- extract-query
  [query-str]
  (try+
    (when query-str
      (json/parse-string query-str))
    (catch Throwable _
      (throw+ {:type   :invalid-argument
               :reason "query string must contain valid JSON"
               :arg    "q"
               :val    query-str}))))


(defn- extract-tags
  [user tags-str]
  (try+
    (if tags-str
      (search/filter-user-tags user (map #(UUID/fromString %) (string/split tags-str #",")))
      [])
    (catch Throwable _
      (throw+ {:type   :invalid-argument
               :reason "must be a comma-separated list of UUIDs"
               :arg    "tags"
               :val    tags-str}))))


(defn qualify-name
  "Qualifies a user or group name with the default zone."
  [name]
  (str name \# (cfg/irods-zone)))


(defn add-timing
  [result start]
  (let [curr-time  (l/local-now)
        t-interval (t/in-millis (t/interval start curr-time))]
    (assoc result :execution-time t-interval)))


; TODO make this work for users that belong to zones other than the default one.
(defn- list-user-groups
  "Looks up the groups a user belongs to. The result is a set of zone-qualified group names.
   Unqualified user names are assumed to belong to the default zone."
  [user]
  (map qualify-name
       (data/list-user-groups (string/replace user (str \# (cfg/irods-zone)) ""))))


(defn ^IPersistentMap search
  "Performs a search on the Elastic Search repository. The search will be filtered to only show what
   the user is allowed to see.

   Parameters:
     user      - the username of the user making the query
     query-str - the JSON query
     tags-str  - the comma-separated list of tag UUIDs to filter the query with. Results will be
                 tagged with at least on of these tags.
     opts      - the optional parameters.

   Returns:
     It returns the response as a map."
  [^String user ^String query-str ^String tags-str ^IPersistentMap opts]
  (try+
    (let [start       (l/local-now)
          query       (extract-query query-str)
          tags        (extract-tags user tags-str)
          type        (extract-type opts)
          offset      (extract-uint opts :offset 0)
          limit       (extract-uint opts :limit (cfg/default-search-result-limit))
          sort        (extract-sort opts [[:score] :desc])
          memberships (conj (list-user-groups user) user)
          query-req   (mk-data-query query tags memberships)
          sort-req    (mk-sort sort)]
      (-> (send-request type query-req offset limit sort-req)
        (extract-result offset memberships)
        (add-timing start)
        svc/success-response))
    (catch [:type :invalid-query] {:keys []}
      (invalid-arg-response "q" query-str "This is not a valid elasticsearch query."))))

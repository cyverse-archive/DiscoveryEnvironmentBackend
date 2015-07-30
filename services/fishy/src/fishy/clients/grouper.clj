(ns fishy.clients.grouper
  (:use [medley.core :only [remove-vals]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [fishy.util.config :as config]))

(def ^:private content-type "text/x-json")

(defn- grouper-uri
  [& components]
  (str (apply curl/url (config/grouper-base) "servicesRest" (config/grouper-api-version)
              components)))

(defn- act-as-subject-lookup
  ([username]
     {:subjectId username})
  ([]
     (act-as-subject-lookup "GrouperSystem")))

(defn- group-search-query-filter
  [stem name]
  (remove-vals nil? {:groupName       name
                     :queryFilterType "FIND_BY_GROUP_NAME_APPROXIMATE"
                     :stemName        stem}))

(defn- format-group-search-request
  [stem name]
  (-> {:WsRestFindGroupsRequest
       {:actAsSubjectLookup (act-as-subject-lookup)
        :wsQueryFilter      (group-search-query-filter stem name)}}
      (json/encode true)))

(defn group-search
  [stem name]
  (->> {:body         (format-group-search-request stem name)
        :basic-auth   [(config/grouper-username) (config/grouper-password)]
        :content-type content-type
        :as           :json}
       (http/post (grouper-uri "groups"))
       (:body)
       (:WsFindGroupsResults)
       (:groupResults)))

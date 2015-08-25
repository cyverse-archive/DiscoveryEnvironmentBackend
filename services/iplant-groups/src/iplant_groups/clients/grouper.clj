(ns iplant_groups.clients.grouper
  (:use [medley.core :only [remove-vals]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [iplant_groups.util.config :as config]
            [iplant_groups.util.service :as service]))

(def ^:private content-type "text/x-json")

(def ^:private default-act-as-subject-id "GrouperSystem")

(defn- auth-params
  []
  (vector (config/grouper-username) (config/grouper-password)))

(defn- build-error-object
  [error-code body]
  (let [result-metadata (:resultMetadata (val (first body)))]
    {:error_code             error-code
     :grouper_result_code    (:resultCode result-metadata)
     :grouper_result_message (:resultMessage result-metadata)}))

(defn- default-error-handler
  [error-code {:keys [body] :as response}]
  (log/warn "Grouper request failed:" response)
  (throw+ (build-error-object error-code (service/parse-json body))))

(defmacro ^:private with-trap
  [[handle-error] & body]
  `(try+
    (do ~@body)
    (catch [:status 400] bad-request#
      (~handle-error ce/ERR_BAD_REQUEST bad-request#))
    (catch [:status 404] not-found#
      (~handle-error ce/ERR_NOT_FOUND not-found#))
    (catch [:status 500] server-error#
      (~handle-error ce/ERR_REQUEST_FAILED server-error#))))

(defn- grouper-uri
  [& components]
  (str (apply curl/url (config/grouper-base) "servicesRest" (config/grouper-api-version)
              components)))

(defn- act-as-subject-lookup
  ([username]
     {:subjectId (or username default-act-as-subject-id)})
  ([]
     (act-as-subject-lookup default-act-as-subject-id)))

;; Group search.

(defn- group-search-query-filter
  [stem name]
  (remove-vals nil? {:groupName       name
                     :queryFilterType "FIND_BY_GROUP_NAME_APPROXIMATE"
                     :stemName        stem}))

(defn- format-group-search-request
  [username stem name]
  (-> {:WsRestFindGroupsRequest
       {:actAsSubjectLookup (act-as-subject-lookup username)
        :wsQueryFilter      (group-search-query-filter stem name)}}
      (json/encode)))

(defn group-search
  [username stem name]
  (with-trap [default-error-handler]
    (->> {:body         (format-group-search-request username stem name)
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "groups"))
         (:body)
         (:WsFindGroupsResults)
         (:groupResults))))

;; Group retrieval.

(defn- group-retrieval-query-filter
  [group-id]
  (remove-vals nil? {:groupUuid       group-id
                     :queryFilterType "FIND_BY_GROUP_UUID"}))

(defn- format-group-retrieval-request
  [username group-id]
  (-> {:WsRestFindGroupsRequest
       {:actAsSubjectLookup (act-as-subject-lookup username)
        :wsQueryFilter      (group-retrieval-query-filter group-id)
        :includeGroupDetail "T"}}
      (json/encode)))

(defn get-group
  [username group-id]
  (with-trap [default-error-handler]
    (->> {:body         (format-group-retrieval-request username group-id)
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "groups"))
         (:body)
         (:WsFindGroupsResults)
         (:groupResults)
         (first))))

;; Group membership listings.

(defn- group-membership-listing-error-handler
  [group-id error-code {:keys [body] :as response}]
  (log/warn "Grouper request failed:" response)
  (let [body    (service/parse-json body)
        get-grc (fn [m] (-> m :WsGetMembersResults :results first :resultMetadata :resultCode))]
    (if (and (= error-code ce/ERR_REQUEST_FAILED) (= (get-grc body) "GROUP_NOT_FOUND"))
      (service/not-found "group" group-id)
      (throw+ (build-error-object error-code body)))))

(defn- format-group-member-listing-request
  [username group-id]
  (->> {:WsRestGetMembersRequest
        {:actAsSubjectLookup (act-as-subject-lookup username)
         :wsGroupLookups     [{:uuid group-id}]}}
       (json/encode)))

(defn get-group-members
  [username group-id]
  (with-trap [(partial group-membership-listing-error-handler group-id)]
    (let [response (->> {:body         (format-group-member-listing-request username group-id)
                         :basic-auth   (auth-params)
                         :content-type content-type
                         :as           :json}
                        (http/post (grouper-uri "groups"))
                        (:body)
                        (:WsGetMembersResults))]
      [(:wsSubjects (first (:results response))) (:subjectAttributeNames response)])))

;; Folder search.

(defn- folder-search-query-filter
  [name]
  (remove-vals nil? {:stemName            name
                     :stemQueryFilterType "FIND_BY_STEM_NAME_APPROXIMATE"}))

(defn- format-folder-search-request
  [username name]
  (-> {:WsRestFindStemsRequest
       {:actAsSubjectLookup (act-as-subject-lookup username)
        :wsStemQueryFilter  (folder-search-query-filter name)}}
      (json/encode)))

(defn folder-search
  [username name]
  (with-trap [default-error-handler]
    (->> {:body         (format-folder-search-request username name)
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "stems"))
         (:body)
         (:WsFindStemsResults)
         (:stemResults))))

;; Subject search.

(defn- format-subject-search-request
  [username search-string]
  (-> {:WsRestGetSubjectsRequest
       {:actAsSubjectLookup (act-as-subject-lookup username)
        :searchString       search-string}}
      (json/encode)))

(defn subject-search
  [username search-string]
  (with-trap [default-error-handler]
    (let [response (->> {:body         (format-subject-search-request username search-string)
                         :basic-auth   (auth-params)
                         :content-type content-type
                         :as           :json}
                        (http/post (grouper-uri "subjects"))
                        (:body)
                        (:WsGetSubjectsResults))]
      [(:wsSubjects response) (:subjectAttributeNames response)])))

;; Subject retrieval.

(defn- subject-id-lookup
  [subject-id]
  (remove-vals nil? {:subjectId subject-id}))

(defn- format-subject-id-lookup-request
  [username subject-id]
  (-> {:WsRestGetSubjectsRequest
       {:actAsSubjectLookup (act-as-subject-lookup username)
        :wsSubjectLookups   [(subject-id-lookup subject-id)]}}
      (json/encode)))

(defn get-subject
  [username subject-id]
  (with-trap [default-error-handler]
    (let [response (->> {:body         (format-subject-id-lookup-request username subject-id)
                         :basic-auth   (auth-params)
                         :content-type content-type
                         :as           :json}
                        (http/post (grouper-uri "subjects"))
                        (:body)
                        (:WsGetSubjectsResults))]
      [(first (:wsSubjects response)) (:subjectAttributeNames response)])))

;; Groups for a subject.

(defn- format-groups-for-subject-request
  [username subject-id]
  (-> {:WsRestGetGroupsRequest
       {:actAsSubjectLookup   (act-as-subject-lookup username)
        :subjectLookups       [(subject-id-lookup subject-id)]}}
      (json/encode)))

(defn groups-for-subject
  [username subject-id]
  (with-trap [default-error-handler]
    (->> {:body         (format-groups-for-subject-request username subject-id)
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "subjects"))
         (:body)
         (:WsGetGroupsResults)
         (:results)
         (first)
         (:wsGroups))))

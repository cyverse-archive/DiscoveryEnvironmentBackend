(ns fishy.clients.grouper
  (:use [medley.core :only [remove-vals]]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [cemerick.url :as curl]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [fishy.util.config :as config]
            [fishy.util.service :as service]))

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

(defn- handle-error
  [error-code {:keys [body] :as response}]
  (log/warn "Grouper request failed:" response)
  (throw+ (build-error-object error-code (service/parse-json body))))

(defmacro ^:private with-trap
  [& body]
  `(try+
    (do ~@body)
    (catch [:status 400] bad-request#
      (handle-error ce/ERR_BAD_REQUEST bad-request#))
    (catch [:status 404] not-found#
      (handle-error ce/ERR_NOT_FOUND not-found#))
    (catch [:status 500] server-error#
      (handle-error ce/ERR_REQUEST_FAILED server-error#))))

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
      (json/encode true)))

(defn group-search
  [username stem name]
  (with-trap
    (->> {:body         (format-group-search-request username stem name)
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "groups"))
         (:body)
         (:WsFindGroupsResults)
         (:groupResults))))

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
      (json/encode true)))

(defn folder-search
  [username name]
  (with-trap
    (->> {:body         (format-folder-search-request username name)
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "stems"))
         (:body)
         (:WsFindStemsResults)
         (:stemResults))))

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

;; Group add/update

(defn- format-group-add-update-request
  [group-lookup update? username type name display-extension description]
  (-> {:WsRestGroupSaveRequest
       {:actAsSubjectLookup (act-as-subject-lookup username)
        :wsGroupToSaves [
         {:wsGroup
          (remove-vals nil? {:name name
                             :description description
                             :displayExtension display-extension
                             :typeOfGroup type})
          :wsGroupLookup group-lookup
          :saveMode (if update? "UPDATE" "INSERT")}
        ]
        :includeGroupDetail "T"}}
      (json/encode)))

(defn- format-group-add-request
  [username type name display-extension description]
  (format-group-add-update-request
    {:groupName name}
    false username type name display-extension description))

(defn- format-group-update-request
  [username uuid name display-extension description]
  (format-group-add-update-request
    {:uuid uuid}
    true username nil name display-extension description)) ;; nil is for 'type' which we shouldn't change for now

(defn- add-update-group
  [request-body]
  (with-trap [default-error-handler]
    (->> {:body         request-body
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "groups"))
         (:body)
         (:WsGroupSaveResults)
         (:results)
         (first)
         (:wsGroup))))

(defn add-group
  [username type name display-extension description]
  (add-update-group
    (format-group-add-request username type name display-extension description)))

(defn update-group
  [username uuid name display-extension description]
  (add-update-group
    (format-group-update-request username uuid name display-extension description)))

;; Group delete

(defn- format-group-delete-request
  [username group-id]
  (-> {:WsRestGroupDeleteRequest
       {:actAsSubjectLookup (act-as-subject-lookup username)
        :wsGroupLookups [
         {:uuid group-id}]}}
      (json/encode)))

(defn delete-group
  [username group-id]
  (with-trap [default-error-handler]
    (->> {:body         (format-group-delete-request username group-id)
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "groups"))
         (:body)
         (:WsGroupDeleteResults)
         (:results)
         (first)
         (:wsGroup))))

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

;; Folder retrieval.

(defn- folder-retrieval-query-filter
  [folder-id]
  {:stemUuid            folder-id
   :stemQueryFilterType "FIND_BY_STEM_UUID"})

(defn- format-folder-retrieval-request
  [username folder-id]
  (-> {:WsRestFindStemsRequest
       {:actAsSubjectLookup (act-as-subject-lookup username)
        :wsStemQueryFilter  (folder-retrieval-query-filter folder-id)}}
      (json/encode)))

(defn get-folder
  [username folder-id]
  (with-trap [default-error-handler]
    (->> {:body         (format-folder-retrieval-request username folder-id)
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "stems"))
         (:body)
         (:WsFindStemsResults)
         (:stemResults)
         (first))))

;; Folder add.

(defn- folder-forbidden-error-handler
  [result-key folder-id error-code {:keys [body] :as response}]
  (log/warn "Grouper request failed:" response)
  (let [body    (service/parse-json body)
        get-grc (fn [m] (-> m result-key :results first :resultMetadata :resultCode))]
    (if (and (= error-code ce/ERR_REQUEST_FAILED) (= (get-grc body) "INSUFFICIENT_PRIVILEGES"))
      (service/forbidden "folder" folder-id)
      (throw+ (build-error-object error-code body)))))

(defn- format-folder-add-update-request
  [stem-lookup update? username name display-extension description]
  (-> {:WsRestStemSaveRequest
       {:actAsSubjectLookup (act-as-subject-lookup username)
        :wsStemToSaves [
         {:wsStem
          (remove-vals nil? {:name name
                             :description description
                             :displayExtension display-extension})
          :wsStemLookup stem-lookup
          :saveMode (if update? "UPDATE" "INSERT")}
        ]}}
      (json/encode)))

(defn- format-folder-add-request
  [username name display-extension description]
  (format-folder-add-update-request
    {:stemName name}
    false username name display-extension description))

(defn- format-folder-update-request
  [username uuid name display-extension description]
  (format-folder-add-update-request
    {:uuid uuid}
    true username name display-extension description))

(defn- add-update-folder
  [request-body name]
  (with-trap [(partial folder-forbidden-error-handler :WsStemSaveResults name)]
    (->> {:body         request-body
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "stems"))
         (:body)
         (:WsStemSaveResults)
         (:results)
         (first)
         (:wsStem))))

(defn add-folder
  [username name display-extension description]
  (add-update-folder
    (format-folder-add-request username name display-extension description) name))

(defn update-folder
  [username uuid name display-extension description]
  (add-update-folder
    (format-folder-update-request username uuid name display-extension description) uuid))

;; Folder delete

(defn- format-folder-delete-request
  [username folder-id]
  (-> {:WsRestStemDeleteRequest
       {:actAsSubjectLookup (act-as-subject-lookup username)
        :wsStemLookups [
         {:uuid folder-id}]}}
      (json/encode)))

(defn delete-folder
  [username folder-id]
  (with-trap [(partial folder-forbidden-error-handler :WsStemDeleteResults folder-id)]
    (->> {:body         (format-folder-delete-request username folder-id)
          :basic-auth   (auth-params)
          :content-type content-type
          :as           :json}
         (http/post (grouper-uri "stems"))
         (:body)
         (:WsStemDeleteResults)
         (:results)
         (first)
         (:wsStem))))

;; Get group/folder privileges

;; This is only available as a Lite request; ActAsSubject works differently.
(defn- format-group-folder-privileges-lookup-request
  [entity-type username group-or-folder-id]
  (if-let [uuid-key (get {:group :groupUuid
                          :folder :stemUuid}
                         entity-type)]
    (-> {:WsRestGetGrouperPrivilegesLiteRequest
         {:actAsSubjectId username
          uuid-key group-or-folder-id}}
        (json/encode))
    (throw+ {:error_code ce/ERR_BAD_REQUEST :entity-type entity-type})))

(defn- get-group-folder-privileges
  [entity-type username group-or-folder-id]
  (with-trap [default-error-handler]
    (let [response (->> {:body         (format-group-folder-privileges-lookup-request entity-type username group-or-folder-id)
                         :basic-auth   (auth-params)
                         :content-type content-type
                         :as           :json}
                        (http/post (grouper-uri "grouperPrivileges"))
                        (:body)
                        (:WsGetGrouperPrivilegesLiteResult))]
      [(:privilegeResults response) (:subjectAttributeNames response)])))

(defn get-group-privileges
  [username group-id]
  (get-group-folder-privileges :group username group-id))

(defn get-folder-privileges
  [username folder-id]
  (get-group-folder-privileges :folder username folder-id))

;; Add/remove group/folder privileges

(defn- format-group-folder-privileges-add-remove-request
  [entity-lookup allowed? username subject-id privilege-names]
  (-> {:WsRestAssignGrouperPrivilegesRequest
       (assoc entity-lookup
         :actAsSubjectLookup (act-as-subject-lookup username)
         :clientVersion "v2_2_000"
         :privilegeNames privilege-names
         :allowed (if allowed? "T" "F")
         :wsSubjectLookups [{:subjectId subject-id}])}
      (json/encode)))

(defn- format-group-privileges-add-remove-request
  [allowed? username group-id subject-id privilege-names]
  (format-group-folder-privileges-add-remove-request
    {:wsGroupLookup {:uuid group-id}}
    allowed? username subject-id privilege-names))

(defn- format-folder-privileges-add-remove-request
  [allowed? username folder-id subject-id privilege-names]
  (format-group-folder-privileges-add-remove-request
    {:wsStemLookup {:uuid folder-id}}
    allowed? username subject-id privilege-names))

(defn- add-remove-group-folder-privileges
  [request-body]
  (with-trap [default-error-handler]
    (let [response (->> {:body         request-body
                         :basic-auth   (auth-params)
                         :content-type content-type
                         :as           :json}
                        (http/post (grouper-uri "grouperPrivileges"))
                        (:body)
                        (:WsAssignGrouperPrivilegesResults))]
      [(first (:results response)) (:subjectAttributeNames response)])))

(defn- add-remove-group-privileges
  [allowed? username group-id subject-id privilege-names]
  (add-remove-group-folder-privileges
    (format-group-privileges-add-remove-request allowed? username group-id subject-id privilege-names)))

(defn- add-remove-folder-privileges
  [allowed? username folder-id subject-id privilege-names]
  (add-remove-group-folder-privileges
    (format-folder-privileges-add-remove-request allowed? username folder-id subject-id privilege-names)))

(defn add-group-privileges
  [username group-id subject-id privilege-names]
  (add-remove-group-privileges true username group-id subject-id privilege-names))

(defn remove-group-privileges
  [username group-id subject-id privilege-names]
  (add-remove-group-privileges false username group-id subject-id privilege-names))

(defn add-folder-privileges
  [username folder-id subject-id privilege-names]
  (add-remove-folder-privileges true username folder-id subject-id privilege-names))

(defn remove-folder-privileges
  [username folder-id subject-id privilege-names]
  (add-remove-folder-privileges false username folder-id subject-id privilege-names))

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

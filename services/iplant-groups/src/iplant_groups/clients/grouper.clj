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

(defn- grouper-post
  [body & uri-parts]
  (->> {:body         (json/encode body)
        :basic-auth   (auth-params)
        :content-type content-type
        :as           :json}
       (http/post (apply grouper-uri uri-parts))
       (:body)))

(defn grouper-ok?
  []
  (try+
    (http/get (str (curl/url (config/grouper-base) "status")) {:query-params {:diagnosticType "sources"}})
    true
    (catch Object err
      (log/warn "Grouper diagnostic check failed:" err)
      false)))

(defn- act-as-subject-lookup
  ([username]
     {:subjectId (or username default-act-as-subject-id)})
  ([]
     (act-as-subject-lookup default-act-as-subject-id)))

(defn- parse-boolean
  [bool-str]
  (when bool-str (Boolean/parseBoolean bool-str)))

;; Group search.

(defn- group-search-query-filter
  [stem name]
  (remove-vals nil? {:groupName       name
                     :queryFilterType "FIND_BY_GROUP_NAME_APPROXIMATE"
                     :stemName        stem}))

(defn- format-group-search-request
  [username stem name]
  {:WsRestFindGroupsRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :wsQueryFilter      (group-search-query-filter stem name)}})

(defn group-search
  [username stem name]
  (with-trap [default-error-handler]
    (-> (format-group-search-request username stem name)
        (grouper-post "groups")
        :WsFindGroupsResults
        :groupResults)))

;; Group retrieval.

(defn- group-retrieval-query-filter
  [group-id]
  (remove-vals nil? {:groupUuid       group-id
                     :queryFilterType "FIND_BY_GROUP_UUID"}))

(defn- format-group-retrieval-request
  [username group-id]
  {:WsRestFindGroupsRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :wsQueryFilter      (group-retrieval-query-filter group-id)
    :includeGroupDetail "T"}})

(defn get-group
  [username group-id]
  (with-trap [default-error-handler]
    (-> (format-group-retrieval-request username group-id)
        (grouper-post "groups")
        :WsFindGroupsResults
        :groupResults
        first)))

;; Group add/update

(defn- format-group-add-update-request
  [group-lookup update? username type name display-extension description]
  {:WsRestGroupSaveRequest
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
    :includeGroupDetail "T"}})

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
    (-> (grouper-post request-body "groups")
        :WsGroupSaveResults
        :results
        first
        :wsGroup)))

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
  {:WsRestGroupDeleteRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :wsGroupLookups [
     {:uuid group-id}]}})

(defn delete-group
  [username group-id]
  (with-trap [default-error-handler]
    (-> (format-group-delete-request username group-id)
        (grouper-post "groups")
        :WsGroupDeleteResults
        :results
        first
        :wsGroup)))

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
  {:WsRestGetMembersRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :wsGroupLookups     [{:uuid group-id}]}})

(defn get-group-members
  [username group-id]
  (with-trap [(partial group-membership-listing-error-handler group-id)]
    (let [response (-> (format-group-member-listing-request username group-id)
                       (grouper-post "groups")
                       :WsGetMembersResults)]
      [(:wsSubjects (first (:results response))) (:subjectAttributeNames response)])))

;; Folder search.

(defn- folder-search-query-filter
  [name]
  (remove-vals nil? {:stemName            name
                     :stemQueryFilterType "FIND_BY_STEM_NAME_APPROXIMATE"}))

(defn- format-folder-search-request
  [username name]
  {:WsRestFindStemsRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :wsStemQueryFilter  (folder-search-query-filter name)}})

(defn folder-search
  [username name]
  (with-trap [default-error-handler]
    (-> (format-folder-search-request username name)
        (grouper-post "stems")
        :WsFindStemsResults
        :stemResults)))

;; Folder retrieval.

(defn- folder-retrieval-query-filter
  [folder-id]
  {:stemUuid            folder-id
   :stemQueryFilterType "FIND_BY_STEM_UUID"})

(defn- format-folder-retrieval-request
  [username folder-id]
  {:WsRestFindStemsRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :wsStemQueryFilter  (folder-retrieval-query-filter folder-id)}})

(defn get-folder
  [username folder-id]
  (with-trap [default-error-handler]
    (-> (format-folder-retrieval-request username folder-id)
        (grouper-post "stems")
        :WsFindStemsResults
        :stemResults
        first)))

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
  {:WsRestStemSaveRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :wsStemToSaves [
     {:wsStem
      (remove-vals nil? {:name name
                         :description description
                         :displayExtension display-extension})
      :wsStemLookup stem-lookup
      :saveMode (if update? "UPDATE" "INSERT")}
    ]}})

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
    (-> (grouper-post request-body "stems")
        :WsStemSaveResults
        :results
        first
        :wsStem)))

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
  {:WsRestStemDeleteRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :wsStemLookups [
     {:uuid folder-id}]}})

(defn delete-folder
  [username folder-id]
  (with-trap [(partial folder-forbidden-error-handler :WsStemDeleteResults folder-id)]
    (-> (format-folder-delete-request username folder-id)
        (grouper-post "stems")
        :WsStemDeleteResults
        :results
        first
        :wsStem)))

;; Get group/folder privileges

;; This is only available as a Lite request; ActAsSubject works differently.
(defn- format-group-folder-privileges-lookup-request
  [entity-type username group-or-folder-id]
  (if-let [uuid-key (get {:group :groupUuid
                          :folder :stemUuid}
                         entity-type)]
    {:WsRestGetGrouperPrivilegesLiteRequest
     {:actAsSubjectId username
      uuid-key group-or-folder-id}}
    (throw+ {:error_code ce/ERR_BAD_REQUEST :entity-type entity-type})))

(defn- get-group-folder-privileges
  [entity-type username group-or-folder-id]
  (with-trap [default-error-handler]
    (let [response (-> (format-group-folder-privileges-lookup-request entity-type username group-or-folder-id)
                       (grouper-post "grouperPrivileges")
                       :WsGetGrouperPrivilegesLiteResult)]
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
  {:WsRestAssignGrouperPrivilegesRequest
   (assoc entity-lookup
     :actAsSubjectLookup (act-as-subject-lookup username)
     :clientVersion "v2_2_000"
     :privilegeNames privilege-names
     :allowed (if allowed? "T" "F")
     :wsSubjectLookups [{:subjectId subject-id}])})

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
    (let [response (-> (grouper-post request-body "grouperPrivileges")
                       :WsAssignGrouperPrivilegesResults)]
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
  {:WsRestGetSubjectsRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :searchString       search-string}})

(defn subject-search
  [username search-string]
  (with-trap [default-error-handler]
    (let [response (-> (format-subject-search-request username search-string)
                       (grouper-post "subjects")
                       :WsGetSubjectsResults)]
      [(:wsSubjects response) (:subjectAttributeNames response)])))

;; Subject retrieval.

(defn- subject-id-lookup
  [subject-id]
  (remove-vals nil? {:subjectId subject-id}))

(defn- format-subject-id-lookup-request
  [username subject-id]
  {:WsRestGetSubjectsRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :wsSubjectLookups   [(subject-id-lookup subject-id)]}})

(defn get-subject
  [username subject-id]
  (with-trap [default-error-handler]
    (let [response (-> (format-subject-id-lookup-request username subject-id)
                       (grouper-post "subjects")
                       :WsGetSubjectsResults)]
      [(first (:wsSubjects response)) (:subjectAttributeNames response)])))

;; Groups for a subject.

(defn- format-groups-for-subject-request
  [username subject-id]
  {:WsRestGetGroupsRequest
   {:actAsSubjectLookup   (act-as-subject-lookup username)
    :subjectLookups       [(subject-id-lookup subject-id)]}})

(defn groups-for-subject
  [username subject-id]
  (with-trap [default-error-handler]
    (-> (format-groups-for-subject-request username subject-id)
        (grouper-post "subjects")
        :WsGetGroupsResults
        :results
        first
        :wsGroups)))

;; Attribute Definition Name search
(defn- format-attribute-name-search-request
  [username search exact?]
  (let [query (if exact?
                  {:wsAttributeDefNameLookups [{:name search}]}
                  {:scope search})]
    {:WsRestFindAttributeDefNamesRequest
      (assoc query
             :actAsSubjectLookup (act-as-subject-lookup username))}))

(defn attribute-name-search
  [username search exact?]
  (with-trap [default-error-handler]
    (-> (format-attribute-name-search-request username search exact?)
        (grouper-post "attributeDefNames")
        :WsFindAttributeDefNamesResults
        :attributeDefNameResults)))

;; Attribute Definition Name add/update

(defn- format-attribute-name-add-update-request ;; functionally add-only to start. need to add a wsAttributeDefNameLookup for update
  [update? username attribute-def-id name display-extension description]
  {:WsRestAttributeDefNameSaveRequest
   {:actAsSubjectLookup (act-as-subject-lookup username)
    :wsAttributeDefNameToSaves [
     {:wsAttributeDefName
      (remove-vals nil? {:attributeDefId attribute-def-id
                         :name name
                         :description description
                         :displayExtension display-extension})
      :saveMode (if update? "UPDATE" "INSERT")}]}})

(defn- format-attribute-name-add-request
  [username attribute-def-id name display-extension description]
  (format-attribute-name-add-update-request false username attribute-def-id name display-extension description))

(defn- add-update-attribute-name
  [request-body]
  (with-trap [default-error-handler]
    (-> (grouper-post request-body "attributeDefNames")
        :WsAttributeDefNameSaveResults
        :results
        first
        :wsAttributeDefName)))

(defn add-attribute-name
  [username attribute-def-id name display-extension description]
  (add-update-attribute-name
    (format-attribute-name-add-request username attribute-def-id name display-extension description)))

;; Permission assignment
;; search/lookup
(defn- format-permission-search-request
  [username attribute-def-id attribute-name-id role-id subject-id action-names immediate-only]
  {:WsRestGetPermissionAssignmentsRequest
   (remove-vals nil? {:actAsSubjectLookup (act-as-subject-lookup username)
                      :wsAttributeDefLookups (if attribute-def-id [{:uuid attribute-def-id}])
                      :wsAttributeDefNameLookups (if attribute-name-id [{:uuid attribute-name-id}])
                      :roleLookups (if role-id [{:uuid role-id}])
                      :actions (if (seq action-names) action-names)
                      :wsSubjectLookups (if subject-id [{:subjectId subject-id}])
                      :immediateOnly (parse-boolean immediate-only)})})

(defn permission-assignment-search
  [username attribute-def-id attribute-name-id role-id subject-id action-names immediate-only]
  (with-trap [default-error-handler]
    (-> (format-permission-search-request username attribute-def-id attribute-name-id role-id subject-id action-names immediate-only)
        (grouper-post "permissionAssignments")
        :WsGetPermissionAssignmentsResults
        :wsPermissionAssigns)))

;; assign/remove
(defn- format-permission-assign-remove-request
  "Format request. lookups-and-type should have the permissionType key as well as any lookups necessary for that type (e.g. type role + roleLookups)"
  [assignment? lookups-and-type username attribute-def-name-id allowed? action-names]
  {:WsRestAssignPermissionsRequest
    (assoc lookups-and-type
           :permissionAssignOperation (if assignment? "assign_permission" "remove_permission")
           :actAsSubjectLookup (act-as-subject-lookup username)
           :permissionDefNameLookups [{:uuid attribute-def-name-id}]
           :disallowed (if allowed? "F" "T")
           :actions action-names)})

(defn- format-permission-assign-request
  [& args]
  (apply format-permission-assign-remove-request true args))

(defn- format-permission-remove-request
  [& args]
  (apply format-permission-assign-remove-request false args))

(defn- role-permission
  [role-id]
  {:permissionType "role"
   :roleLookups [
    {:uuid role-id}]})

(defn- membership-permission
  [role-id subject-id]
  {:permissionType "role_subject"
   :subjectRoleLookups [
   {:wsGroupLookup
     {:uuid role-id}
    :wsSubjectLookup
     {:subjectId subject-id}}]})

(defn- format-role-permission-assign-request
  [username attribute-def-name-id role-id allowed? action-names]
  (format-permission-assign-request
    (role-permission role-id)
    username attribute-def-name-id allowed? action-names))

(defn- format-membership-permission-assign-request
  [username attribute-def-name-id role-id subject-id allowed? action-names]
  (format-permission-assign-request
    (membership-permission role-id subject-id)
    username attribute-def-name-id allowed? action-names))

; For remove requests, 'allowed' is ignored. Pass true as a default.
(defn- format-role-permission-remove-request
  [username attribute-def-name-id role-id action-names]
  (format-permission-remove-request
    (role-permission role-id)
    username attribute-def-name-id true action-names))

(defn- format-membership-permission-remove-request
  [username attribute-def-name-id role-id subject-id action-names]
  (format-permission-remove-request
    (membership-permission role-id subject-id)
    username attribute-def-name-id true action-names))

(defn- assign-remove-permission
  [request-body]
  (with-trap [default-error-handler]
    (-> (grouper-post request-body "permissionAssignments")
        :WsAssignPermissionsResults
        :wsAssignPermissionResults
        first
        :wsAttributeAssigns
        first)))

(defn assign-role-permission
  [username attribute-def-name-id role-id allowed? action-names]
  (assign-remove-permission
    (format-role-permission-assign-request
      username attribute-def-name-id role-id allowed? action-names)))

(defn remove-role-permission
  [username attribute-def-name-id role-id action-names]
  (assign-remove-permission
    (format-role-permission-remove-request
      username attribute-def-name-id role-id action-names)))

(defn assign-membership-permission
  [username attribute-def-name-id role-id subject-id allowed? action-names]
  (assign-remove-permission
    (format-membership-permission-assign-request
      username attribute-def-name-id role-id subject-id allowed? action-names)))

(defn remove-membership-permission
  [username attribute-def-name-id role-id subject-id action-names]
  (assign-remove-permission
    (format-membership-permission-remove-request
      username attribute-def-name-id role-id subject-id action-names)))

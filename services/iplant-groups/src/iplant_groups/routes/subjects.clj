(ns iplant_groups.routes.subjects
  (:use [compojure.api.sweet]
        [iplant_groups.routes.domain.group]
        [iplant_groups.routes.domain.params]
        [iplant_groups.routes.domain.subject])
  (:require [iplant_groups.service.subjects :as subjects]
            [iplant_groups.util.service :as service]))

(defroutes* subjects
  (GET* "/" [:as {:keys [uri]}]
        :query       [params SearchParams]
        :return      SubjectList
        :summary     "Subject Search"
        :description "This endpoint allows callers to search for subjects by name."
        (service/trap uri subjects/subject-search params))

  (GET* "/:subject-id" [:as {:keys [uri]}]
        :path-params [subject-id :- SubjectIdPathParam]
        :query       [params SecuredQueryParams]
        :return      Subject
        :summary     "Get Subject Information"
        :description "This endpoint allows callers to get information about a single subject."
        (service/trap uri subjects/get-subject subject-id params))

  (GET* "/:subject-id/groups" [:as {:keys [uri]}]
        :path-params [subject-id :- SubjectIdPathParam]
        :query       [params SecuredQueryParams]
        :return      GroupList
        :summary     "List Groups for a Subject"
        :description "This endpoint allows callers to list all groups that a subject belongs to."
        (service/trap uri subjects/groups-for-subject subject-id params)))

(ns iplant_groups.routes.subjects
  (:use [common-swagger-api.schema]
        [iplant_groups.routes.domain.group]
        [iplant_groups.routes.domain.params]
        [iplant_groups.routes.domain.subject]
        [ring.util.http-response :only [ok]])
  (:require [iplant_groups.service.subjects :as subjects]))

(defroutes* subjects
  (GET* "/" []
        :query       [params SearchParams]
        :return      SubjectList
        :summary     "Subject Search"
        :description "This endpoint allows callers to search for subjects by name."
        (ok (subjects/subject-search params)))

  (GET* "/:subject-id" []
        :path-params [subject-id :- SubjectIdPathParam]
        :query       [params StandardUserQueryParams]
        :return      Subject
        :summary     "Get Subject Information"
        :description "This endpoint allows callers to get information about a single subject."
        (ok (subjects/get-subject subject-id params)))

  (GET* "/:subject-id/groups" []
        :path-params [subject-id :- SubjectIdPathParam]
        :query       [params StandardUserQueryParams]
        :return      GroupList
        :summary     "List Groups for a Subject"
        :description "This endpoint allows callers to list all groups that a subject belongs to."
        (ok (subjects/groups-for-subject subject-id params))))

(ns fishy.routes.subjects
  (:use [compojure.api.sweet]
        [fishy.routes.domain.group]
        [fishy.routes.domain.params]
        [fishy.routes.domain.subject])
  (:require [fishy.service.subjects :as subjects]
            [fishy.util.service :as service]))

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

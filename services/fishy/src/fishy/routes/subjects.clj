(ns fishy.routes.subjects
  (:use [compojure.api.sweet]
        [fishy.routes.domain.subject]
        [fishy.routes.domain.params])
  (:require [fishy.service.subjects :as subjects]
            [fishy.util.service :as service]))

(defroutes* subjects
  (GET* "/" [:as {:keys [uri]}]
        :query       [params SearchParams]
        :return      SubjectList
        :summary     "Subject Search"
        :description "This endpoint allows callers to search for subjects by name."
        (service/trap uri subjects/subject-search params)))

(ns fishy.routes.groups
  (:use [compojure.api.sweet]
        [fishy.routes.domain.group]
        [fishy.routes.domain.params])
  (:require [fishy.service.groups :as groups]
            [fishy.util.service :as service]))

(defroutes* groups
  (GET* "/" [:as {:keys [uri]}]
        :query       [params SearchParams]
        :return      GroupList
        :summary     "Group Search"
        :description "This endpoint allows callers to search for groups by name."
        (service/trap uri groups/group-search params)))

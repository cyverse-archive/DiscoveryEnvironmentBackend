(ns iplant_groups.routes.groups
  (:use [common-swagger-api.schema]
        [iplant_groups.routes.domain.group]
        [iplant_groups.routes.domain.params])
  (:require [iplant_groups.service.groups :as groups]
            [iplant_groups.util.service :as service]))

(defroutes* groups
  (GET* "/" [:as {:keys [uri]}]
        :query       [params GroupSearchParams]
        :return      GroupList
        :summary     "Group Search"
        :description "This endpoint allows callers to search for groups by name. Only groups that
        are visible to the given user will be listed. The folder name, if provided, contains the
        name of the folder to search. Any folder name provided must exactly match the name of a
        folder in the system."
        (service/trap uri groups/group-search params))

  (POST* "/" [:as {:keys [uri]}]
        :return      GroupWithDetail
        :query       [params StandardUserQueryParams]
        :body        [body (describe BaseGroup "The group to add.")]
        :summary     "Add Group"
        :description "This endpoint allows adding a new group."
        (service/trap uri groups/add-group body params))

  (context* "/:group-id" []
    :path-params [group-id :- GroupIdPathParam]

    (GET* "/" [:as {:keys [uri]}]
          :query       [params StandardUserQueryParams]
          :return      GroupWithDetail
          :summary     "Get Group Information"
          :description "This endpoint allows callers to get detailed information about a single
          group."
          (service/trap uri groups/get-group group-id params))

    (DELETE* "/" [:as {:keys [uri]}]
          :query       [params StandardUserQueryParams]
          :return      Group
          :summary     "Delete Group"
          :description "This endpoint allows deleting a group if the current user has permissions to do so."
          (service/trap uri groups/delete-group group-id params))

    (GET* "/members" [:as {:keys [uri]}]
          :query       [params StandardUserQueryParams]
          :return      GroupMembers
          :summary     "List Group Members"
          :description "This endpoint allows callers to list the members of a single group."
          (service/trap uri groups/get-group-members group-id params))))

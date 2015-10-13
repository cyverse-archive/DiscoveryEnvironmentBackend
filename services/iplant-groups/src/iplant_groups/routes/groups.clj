(ns iplant_groups.routes.groups
  (:use [common-swagger-api.schema]
        [iplant_groups.routes.domain.group]
        [iplant_groups.routes.domain.privileges]
        [iplant_groups.routes.domain.params]
        [ring.util.http-response :only [ok]])
  (:require [iplant_groups.service.groups :as groups]))

(defroutes* groups
  (GET* "/" []
        :query       [params GroupSearchParams]
        :return      GroupList
        :summary     "Group Search"
        :description "This endpoint allows callers to search for groups by name. Only groups that
        are visible to the given user will be listed. The folder name, if provided, contains the
        name of the folder to search. Any folder name provided must exactly match the name of a
        folder in the system."
        (ok (groups/group-search params)))

  (POST* "/" []
        :return      GroupWithDetail
        :query       [params StandardUserQueryParams]
        :body        [body (describe BaseGroup "The group to add.")]
        :summary     "Add Group"
        :description "This endpoint allows adding a new group."
        (ok (groups/add-group body params)))

  (context* "/:group-id" []
    :path-params [group-id :- GroupIdPathParam]

    (GET* "/" []
          :query       [params StandardUserQueryParams]
          :return      GroupWithDetail
          :summary     "Get Group Information"
          :description "This endpoint allows callers to get detailed information about a single
          group."
          (ok (groups/get-group group-id params)))

    (PUT* "/" []
          :return      GroupWithDetail
          :query       [params StandardUserQueryParams]
          :body        [body (describe GroupUpdate "The group information to update.")]
          :summary     "Update Group"
          :description "This endpoint allows callers to update group information."
          (ok (groups/update-group group-id body params)))

    (DELETE* "/" []
          :query       [params StandardUserQueryParams]
          :return      GroupStub
          :summary     "Delete Group"
          :description "This endpoint allows deleting a group if the current user has permissions to do so."
          (ok (groups/delete-group group-id params)))

    (context* "/privileges" []
      (GET* "/" []
            :query       [params StandardUserQueryParams]
            :return      GroupPrivileges
            :summary     "List Group Privileges"
            :description "This endpoint allows callers to list the privileges visible to the current user of a single group."
            (ok (groups/get-group-privileges group-id params)))

      (context* "/:subject-id/:privilege-name" []
        :path-params [subject-id :- NonBlankString
                      privilege-name :- NonBlankString]

        (PUT* "/" []
              :query       [params StandardUserQueryParams]
              :return      Privilege
              :summary     "Add Group Privilege"
              :description "This endpoint allows callers to add a specific privilege for a specific subject to a specific group."
              (ok (groups/add-group-privilege group-id subject-id privilege-name params)))

        (DELETE* "/" []
              :query       [params StandardUserQueryParams]
              :return      Privilege
              :summary     "Remove Group Privilege"
              :description "This endpoint allows callers to remove a specific privilege for a specific subject to a specific group."
              (ok (groups/remove-group-privilege group-id subject-id privilege-name params)))))

    (GET* "/members" []
          :query       [params StandardUserQueryParams]
          :return      GroupMembers
          :summary     "List Group Members"
          :description "This endpoint allows callers to list the members of a single group."
          (ok (groups/get-group-members group-id params)))))

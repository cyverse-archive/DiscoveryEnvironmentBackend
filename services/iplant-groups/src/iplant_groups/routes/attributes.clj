(ns iplant_groups.routes.attributes
  (:use [common-swagger-api.schema]
        [iplant_groups.routes.domain.params]
        [iplant_groups.routes.domain.attribute]
        [ring.util.http-response :only [ok]])
  (:require [iplant_groups.service.attributes :as attributes]))

(defroutes* attributes
  (GET* "/" []
        :query       [params AttributeSearchParams]
        :return      AttributeNameList
        :summary     "Attribute Name/Resource Search"
        :description "This endpoint allows callers to search for attribute names/permission resources by name, or with the 'exact' parameter, to look them up exactly by name."
        (ok (attributes/attribute-search params)))

  (GET* "/permissions" []
        :query       [params AttributeAssignmentSearchParams]
        :return      PermissionAssignmentList
        :summary     "Permissions Search/Lookup"
        :description "This endpoint allows callers to look up permission assignments. Only permissions that are visible to the given user will be listed."
        (ok (attributes/permission-assignment-search params)))

  (POST* "/" []
        :return      AttributeName
        :query       [params StandardUserQueryParams]
        :body        [body (describe BaseAttributeName "The attribute/resource to add.")]
        :summary     "Add Attribute Name/Resource Definition"
        :description "This endpoint allows adding a new attribute name. Grouper also uses attribute names to store resources to which permissions are assigned. An attribute definition must be present, already made by using the Grouper UI (most likely), as there is no web service for creating them."
        (ok (attributes/add-attribute-name body params)))

  (context* "/:attribute-id" []
    :path-params [attribute-id :- AttributeIdPathParam]

    (context* "/permissions" []

      (GET* "/" []
            :query [params StandardUserQueryParams]
            :return PermissionAssignmentList
            :summary "Permissions Lookup"
            :description "This endpoint allows callers to look up all permission assignments for this attribute ID. Only permissions that are visible to the given user will be listed."
            (ok (attributes/permission-assignment-search (assoc params :attribute_def_name_id attribute-id))))

      (context* "/roles/:role-id/:action-name" []
        :path-params [role-id     :- GroupIdPathParam
                      action-name :- NonBlankString]

        (PUT* "/" []
              :return AttributeAssignment
              :body [body PermissionAllowed]
              :query  [params StandardUserQueryParams]
              :summary "Assign Role Permission"
              :description "This endpoint allows assigning a permission to a role (group). The provided group is assigned a permission corresponding to the action-name on the resource defined by this attribute. The 'allowed' query parameter specifies whether the permission assignment is granting the permission or denying the permission (the latter is useful if the role has an inherited permission)."
              (ok (attributes/assign-role-permission params body attribute-id role-id action-name)))

        (DELETE* "/" []
                 :return AttributeAssignment
                 :query  [params StandardUserQueryParams]
                 :summary "Remove Role Permission"
                 :description "This endpoint allows removing a permission to a role (group). The permission corresponding to this attribute, group, action, and allowed/disallowed value is removed, if it exists."
                 (ok (attributes/remove-role-permission params attribute-id role-id action-name))))

      (context* "/memberships/:role-id/:subject-id/:action-name" []
        :path-params [role-id     :- GroupIdPathParam
                      subject-id  :- SubjectIdPathParam
                      action-name :- NonBlankString]

        (PUT* "/" []
              :return AttributeAssignment
              :body [body PermissionAllowed]
              :query  [params StandardUserQueryParams]
              :summary "Assign Membership Permission"
              :description "This endpoint allows assigning a permission to a membership (i.e., a subject in the context of a group). The provided membership is assigned a permission corresponding to the action-name on the resource defined by this attribute. The 'allowed' query parameter specifies whether the permission assignment is granting the permission or denying the permission (the latter is useful if the role has an inherited permission)."
              (ok (attributes/assign-membership-permission params body attribute-id role-id subject-id action-name)))

        (DELETE* "/" []
                 :return AttributeAssignment
                 :query  [params StandardUserQueryParams]
                 :summary "Remove Membership Permission"
                 :description "This endpoint allows removing a permission to a membership (i.e., a subject in the context of a group). The permission corresponding to this attribute, membership, action, and allowed/disallowed value is removed, if it exists."
                 (ok (attributes/remove-membership-permission params attribute-id role-id subject-id action-name)))))))

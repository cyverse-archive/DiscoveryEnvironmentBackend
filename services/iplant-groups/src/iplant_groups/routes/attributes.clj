(ns iplant_groups.routes.attributes
  (:use [common-swagger-api.schema]
        [iplant_groups.routes.domain.params]
        [iplant_groups.routes.domain.attribute]
        [ring.util.http-response :only [ok]])
  (:require [iplant_groups.service.attributes :as attributes]))

(defroutes* attributes
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

      (context* "/roles/:role-id/:action-name" []
        :path-params [role-id     :- GroupIdPathParam
                      action-name :- NonBlankString]

        (PUT* "/" []
              :return AttributeAssignment
              :query  [params PermissionAssignParams]
              :summary "Assign Role Permission"
              :description "This endpoint allows assigning a permission to a role (group). The provided group is assigned a permission corresponding to the action-name on the resource defined by this attribute. The 'allowed' query parameter specifies whether the permission assignment is granting the permission or denying the permission (the latter is useful if the role has an inherited permission)."
              (ok (attributes/assign-role-permission params attribute-id role-id action-name)))

        (DELETE* "/" []
                 :return AttributeAssignment
                 :query  [params PermissionAssignParams]
                 :summary "Remove Role Permission"
                 :description "This endpoint allows removing a permission to a role (group). The permission corresponding to this attribute, group, action, and allowed/disallowed value is removed, if it exists."
                 (ok (attributes/remove-role-permission params attribute-id role-id action-name))))

      (context* "/memberships/:role-id/:subject-id/:action-name" []
        :path-params [role-id     :- GroupIdPathParam
                      subject-id  :- SubjectIdPathParam
                      action-name :- NonBlankString]

        (PUT* "/" []
              :return AttributeAssignment
              :query  [params PermissionAssignParams]
              :summary "Assign Membership Permission"
              :description "This endpoint allows assigning a permission to a membership (i.e., a subject in the context of a group). The provided membership is assigned a permission corresponding to the action-name on the resource defined by this attribute. The 'allowed' query parameter specifies whether the permission assignment is granting the permission or denying the permission (the latter is useful if the role has an inherited permission)."
              (ok (attributes/assign-membership-permission params attribute-id role-id subject-id action-name)))

        (DELETE* "/" []
                 :return AttributeAssignment
                 :query  [params PermissionAssignParams]
                 :summary "Remove Membership Permission"
                 :description "This endpoint allows removing a permission to a membership (i.e., a subject in the context of a group). The permission corresponding to this attribute, membership, action, and allowed/disallowed value is removed, if it exists."
                 (ok (attributes/remove-membership-permission params attribute-id role-id subject-id action-name))))
)))

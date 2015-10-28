(ns iplant_groups.routes.domain.attribute
  (:use [common-swagger-api.schema :only [describe ->optional-param]])
  (:require [schema.core :as s]))

(s/defschema AttributeDefinition
  {:name
   (describe String "The internal attribute-definition name")

   :id
   (describe String "The attribute-definition ID.")})

(s/defschema BaseAttributeName
  {:name
   (describe String "The internal attribute-name name.")

   (s/optional-key :description)
   (describe String "A brief description of the group.")

   (s/optional-key :display_extension)
   (describe String "The displayable attribute-name name extension.")

   :attribute_definition ;; Stub value for adds/updates only.
   {:id (describe String "The attribute-definition ID.")}})

(s/defschema AttributeName
  (assoc BaseAttributeName
   (s/optional-key :display_name)
   (describe String "The displayable attribute-name name.")

   (s/optional-key :extension)
   (describe String "The internal attribute-name name extension.")

   :id_index
   (describe String "The sequential ID index number.")

   :id
   (describe String "The attribute-name ID.")

   :attribute_definition
   (describe AttributeDefinition "This attribute-name's associated attribute-definition.")))

(s/defschema AttributeNameList
  {:attributes (describe [AttributeName] "The list of attribute-name results.")})

(s/defschema AttributeAssignment
  {:id (describe String "The attribute assignment ID")
   :disallowed (describe Boolean "If this assignment is marked as disallowing the permission, rather than allowing it.")
   :enabled (describe Boolean "Whether this permission assignment is enabled")
   :action_id (describe String "The action identifier.")
   :action_name (describe String "The action name (i.e. type of permission) for this assignment.")
   :action_type (describe String "Whether this is immediate or effective.")
   :delegatable (describe Boolean "Whether this assignment is delegatable.")
   :assign_type (describe String "The type of attribute assignment, e.g. any_mem")
   :created_at (describe Long "The date and time the assignment was created (ms since epoch).")
   :modified_at (describe Long "The date and time the assignment was created (ms since epoch).")
   :attribute_definition (describe AttributeDefinition "This assignment's attribute-name's associated attribute-definition.")
   :attribute_definition_name (describe {:id String :name String} "This assignment's attribute-name information.")
   (s/optional-key :group) (describe {:id String :name String} "The group this was assigned to, if relevant.")
   (s/optional-key :membership) (describe {:id String} "The membership this was assigned to, if relevant.")
   (s/optional-key :subject) (describe {:id String :source_id String} "The member/subject this was assigned to, if relevant.")})

(s/defschema PermissionAssignment
  (dissoc AttributeAssignment
          :created_at :modified_at :action_type :assign_type))


(s/defschema PermissionDetail
  {:action_depth                      (describe Integer "The depth of the action-inheritance hierarchy.")
   (s/optional-key :assignment_notes) (describe String "Free-form notes about the assignment.")
   :attribute_def_name_set_depth      (describe Integer "The depth of the attribute-def-name hierarchy.")
   (s/optional-key :disabled_time)    (describe Long "The date and time the permission becomes disabled.")
   (s/optional-key :enabled_time)     (describe Long "The date and time the permission becomes enabled.")
   (s/optional-key :heuristic_friendly_score)
     (describe Integer "An integer to be used for nice sorting.")
   :immediate_membership              (describe Boolean "Whether the subject's membership in the role is immediate.")
   :immediate_permission              (describe Boolean "Whether the permission is immediate to the role or subject.")
   (s/optional-key :member_id)        (describe String "The member ID, if present.")
   :membership_depth                  (describe Integer "The depth of the membership hierarchy.")
   :role_set_depth                    (describe Integer "The depth of the role set hierarchy.")})

(s/defschema PermissionWithDetail
  (assoc PermissionAssignment
         :detail PermissionDetail))

(s/defschema PermissionWithDetailList
  {:assignments (describe [PermissionWithDetail] "The permission assignments.")})

(s/defschema PermissionAllowed
  {:allowed (describe Boolean "Whether this permission should be marked as allowed or disallowed (latter to override an inherited permission).")})

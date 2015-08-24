(ns iplant_groups.routes.domain.group
  (:use [compojure.api.sweet :only [describe]])
  (:require [iplant_groups.routes.domain.params :as params]
            [iplant_groups.routes.domain.subject :as subject]
            [schema.core :as s]))

(s/defschema Group
  {(s/optional-key :description)
   (describe String "A brief description of the group.")

   (s/optional-key :display_extension)
   (describe String "The displayable group name extension.")

   (s/optional-key :display_name)
   (describe String "The displayable group name.")

   (s/optional-key :extension)
   (describe String "The internal group name extension.")

   :id_index
   (describe String "The sequential ID index number.")

   :name
   (describe String "The internal group name.")

   :type
   (describe String "The group type name.")

   :id
   (describe String "The group ID.")})

(s/defschema GroupDetail
  {(s/optional-key :attribute_names)
   (describe [String] "Attribute names, not including the ones listed in the group itself.")

   (s/optional-key :attribute_values)
   (describe [String] "Attribute values, not including the ones listed in the group itself.")

   (s/optional-key :composite_type)
   (describe String "The type of composite group, if applicable.")

   :created_at
   (describe Long "The date and time the group was created (ms since epoch).")

   :created_by
   (describe String "The ID of the subject who created the group.")

   :has_composite
   (describe Boolean "True if this group has a composite member.")

   :is_composite_factor
   (describe Boolean "True if this group is a composite member of another group.")

   (s/optional-key :left_group)
   (describe Group "The left group if this group is a composite.")

   (s/optional-key :modified_at)
   (describe Long "The date and time the group was last modified (ms since epoch).")

   (s/optional-key :modified_by)
   (describe String "The ID of the subject who last modified the group.")

   (s/optional-key :right_group)
   (describe Group "The right group if this group is a composite.")

   (s/optional-key :type_names)
   (describe [String] "The types associated with this group.")})

(s/defschema GroupWithDetail
  (assoc Group
    (s/optional-key :detail)
    (describe GroupDetail "Detailed information about the group.")))

(s/defschema GroupList
  {:groups (describe [Group] "The list of groups in the result set.")})

(s/defschema GroupMembers
  {:members (describe [subject/Subject] "The list of group members.")})

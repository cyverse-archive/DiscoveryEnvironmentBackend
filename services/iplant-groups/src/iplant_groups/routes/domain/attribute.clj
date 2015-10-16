(ns iplant_groups.routes.domain.attribute
  (:use [common-swagger-api.schema :only [describe]])
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

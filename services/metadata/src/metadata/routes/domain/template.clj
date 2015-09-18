(ns metadata.routes.domain.template
  (:use [common-swagger-api.schema :only [describe]])
  (:require [schema.core :as s]
            [metadata.persistence.templates :as tp])
  (:import [java.util Date UUID]))

(def TemplateIdPathParam (describe UUID "The metadata template ID"))
(def AttrIdPathParam (describe UUID "The metadata attribute ID"))
(def ValidValueTypeEnum (describe (apply s/enum (tp/get-value-type-names)) "The attribute's data type"))

(s/defschema MetadataTemplateListEntry
  {:created_by  (describe String "The username of the user who created the template")
   :created_on  (describe Date "The date and time of template creation")
   :deleted     (describe Boolean "True if the template has been marked as deleted")
   :id          (describe UUID "The metadata template ID")
   :modified_by (describe String "The username of the user who most recently modified the template")
   :modified_on (describe Date "The date and time of the most recent template modification")
   :name        (describe String "The metadata template name")})

(s/defschema MetadataTemplateList
  {:metadata_templates (describe [MetadataTemplateListEntry] "The list of metadata templates")})

(s/defschema TemplateAttrEnumValue
  {:id         (describe UUID "The attribute enumeration value ID")
   :is_default (describe Boolean "True if this value is the default for its enumeration type")
   :value      (describe String "The name of the enumeration value")})

(s/defschema MetadataTemplateAttr
  {:id
   (describe UUID "The attribute ID")

   :name
   (describe String "The attribute name")

   :description
   (describe String "A brief description of the attribute")

   :created_by
   (describe String "The username of the user who created the template")

   :created_on
   (describe Date "The date and time of template creation")

   :modified_by
   (describe String "The username of the user who most recently modified the template")

   :modified_on
   (describe Date "The date and time of the most recent template modification")

   :synonyms
   (describe [UUID] "The IDs of synonymous attributes")

   :required
   (describe Boolean "True if the attribute must have a value")

   :type
   ValidValueTypeEnum

   (s/optional-key :values)
   (describe [TemplateAttrEnumValue] "The list of possible values for enumeration types")})

(s/defschema MetadataTemplate
  (assoc MetadataTemplateListEntry
    :attributes
    (describe [MetadataTemplateAttr] "The list of metadata attributes in the template")))

(s/defschema TemplateAttrEnumValueUpdate
  {(s/optional-key :id)
   (describe UUID "The attribute enumeration value ID")

   (s/optional-key :is_default)
   (describe Boolean "True if this value is the default for its enumeration type")

   :value
   (describe String "The name of the enumeration value")})

(s/defschema MetadataTemplateAttrUpdate
  {:description
   (describe String "A brief description of the attribute")

   (s/optional-key :id)
   (describe UUID "The attribute ID")

   :name
   (describe String "The attribute name")

   (s/optional-key :required)
   (describe Boolean "True if the attribute must have a value")

   :type
   ValidValueTypeEnum

   (s/optional-key :values)
   (describe [TemplateAttrEnumValueUpdate] "The list of possible values for enumeration types")})

(s/defschema MetadataTemplateUpdate
  {:attributes
   (describe [MetadataTemplateAttrUpdate] "The list of metadata attributes in the template")

   (s/optional-key :deleted)
   (describe Boolean "True if the template is being marked as deleted.")

   (s/optional-key :id)
   (describe UUID "The attribute ID")

   :name
   (describe String "The metadata template name")})

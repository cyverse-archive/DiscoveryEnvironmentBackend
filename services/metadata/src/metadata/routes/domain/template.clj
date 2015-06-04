(ns metadata.routes.domain.template
  (:use [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s])
  (:import [java.util Date UUID]))

(def TemplateIdPathParam (describe UUID "The metadata template ID"))
(def AttrIdPathParam (describe UUID "The metadata attribute ID"))

(s/defschema MetadataTemplateListEntry
  {:created_by  (describe UUID "The ID of the user who created the template")
   :created_on  (describe Date "The date and time of template creation")
   :deleted     (describe Boolean "True if the template has been marked as deleted")
   :id          (describe UUID "The metadata template ID")
   :modified_by (describe UUID "The ID if the user who most recently modified the template")
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
   (describe UUID "The ID of the user who created the template")

   :created_on
   (describe Date "The date and time of template creation")

   :modified_by
   (describe UUID "The ID if the user who most recently modified the template")

   :modified_on
   (describe Date "The date and time of the most recent template modification")

   :synonyms
   (describe [UUID] "The IDs of synonymous attributes")

   :required
   (describe Boolean "True if the attribute must have a value")

   :type
   (describe String "The attribute data type")

   (s/optional-key :values)
   (describe [TemplateAttrEnumValue] "The list of possible values for enumeration types")})

(s/defschema MetadataTemplate
  (assoc MetadataTemplateListEntry
    :attributes
    (describe [MetadataTemplateAttr] "The list of metadata attributes in the template")))

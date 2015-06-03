(ns metadata.routes.domain.template
  (:use [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s])
  (:import [java.util Date UUID]))

(s/defschema MetadataTemplate
  {:created_by  (describe UUID "The ID of the user who created the template")
   :created_on  (describe Date "The date and time of template creation")
   :deleted     (describe Boolean "True if the template has been marked as deleted")
   :id          (describe UUID "The metadata template ID")
   :modified_by (describe UUID "The ID if the user who most recently modified the template")
   :modified_on (describe Date "The date and time of the most recent template modification")
   :name        (describe String "The template name")})

(s/defschema MetadataTemplates
  {:metadata_templates (describe [MetadataTemplate] "The list of metadata templates")})

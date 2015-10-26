(ns iplant_groups.routes.domain.params
  (:use [common-swagger-api.schema :only [describe NonBlankString StandardUserQueryParams]])
  (:require [clojure.string :as string]
            [schema.core :as s]))

(def SubjectIdPathParam
  (describe String "The subject identifier."))

(def GroupIdPathParam
  (describe String "The group identifier."))

(def FolderIdPathParam
  (describe String "The folder identifier."))

(def AttributeIdPathParam
  (describe String "The attribute-name identifier."))

(s/defschema SearchParams
  (assoc StandardUserQueryParams
    :search (describe NonBlankString "The partial name of the entity to search for.")))

(s/defschema GroupSearchParams
  (assoc SearchParams
    (s/optional-key :folder)
    (describe NonBlankString "The name of the folder to search for.")))

(s/defschema AttributeSearchParams
  (assoc StandardUserQueryParams
    (s/optional-key :attribute_def_id)
    (describe NonBlankString "The id of an attribute/permission definition to search with.")

    (s/optional-key :attribute_def_name_id)
    (describe NonBlankString "The id of an attribute name/permision resource to search with.")

    (s/optional-key :subject_id)
    (describe NonBlankString "The id of a subject to search with.")

    (s/optional-key :role_id)
    (describe NonBlankString "The id of a role-type group to search with.")

    (s/optional-key :action_names)
    (describe [NonBlankString] "A list of action names to search with.")))

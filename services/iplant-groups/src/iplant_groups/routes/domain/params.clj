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

(s/defschema SearchParams
  (assoc StandardUserQueryParams
    :search (describe NonBlankString "The partial name of the entity to search for.")))

(s/defschema GroupSearchParams
  (assoc SearchParams
    (s/optional-key :folder)
    (describe NonBlankString "The name of the folder to search for.")))

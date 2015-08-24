(ns iplant_groups.routes.domain.params
  (:use [compojure.api.sweet :only [describe]])
  (:require [clojure.string :as string]
            [schema.core :as s]))

(def NonBlankString
  (describe
   (s/both String (s/pred (complement string/blank?) `non-blank?))
   "A non-blank string."))

(def SubjectIdPathParam
  (describe String "The subject identifier."))

(def GroupIdPathParam
  (describe String "The group identifier."))

(s/defschema SecuredQueryParams
  {:user (describe NonBlankString "The short version of the username")})

(s/defschema SearchParams
  (assoc SecuredQueryParams
    :search (describe NonBlankString "The partial name of the entity to search for.")))

(s/defschema GroupSearchParams
  (assoc SearchParams
    (s/optional-key :folder)
    (describe NonBlankString "The name of the folder to search for.")))

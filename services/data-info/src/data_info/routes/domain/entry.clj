(ns data-info.routes.domain.entry
  (:use [compojure.api.sweet :only [describe]]
        [data-info.routes.domain.common]
        [heuristomancer.core :as info])
  (:require [schema.core :as s]))

(def ValidSortFields
  #{:datecreated
    :datemodified
    :name
    :path
    :size})

(def ValidInfoTypesEnum (apply s/enum (info/supported-formats)))

(s/defschema FolderListingParams
  (merge
    SecuredQueryParamsRequired
    (assoc PagingParams
      SortFieldOptionalKey
      (describe (apply s/enum ValidSortFields) SortFieldDocs))
    {(s/optional-key :entity-type)
     (describe (s/enum :any :file :folder) "The type of folder items to include in the response.")

     (s/optional-key :bad-chars)
     (describe String
       "A list of characters which will mark a folder item's `badName` field to true if found in
        that item's name.")

     (s/optional-key :bad-name)
     (describe (s/either [String] String)
       "A list of names which will mark a folder item's `badName` field to true if its name matches
        any in the list.")

     (s/optional-key :bad-path)
     (describe (s/either [String] String)
       "A list of paths which will mark a folder item's `badName` field to true if its path matches
        any in the list.")

     (s/optional-key :info-type)
     (describe (s/either [ValidInfoTypesEnum] ValidInfoTypesEnum)
       "A list of info-types with which to filter a folder's result items.")

     (s/optional-key :attachment)
     (describe Boolean "Download file contents as attachment.")}))

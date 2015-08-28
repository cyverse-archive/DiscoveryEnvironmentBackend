(ns data-info.routes.domain.data
  (:use [compojure.api.sweet :only [describe]]
        [data-info.routes.domain.common]
        [heuristomancer.core :as info])
  (:require [schema.core :as s]))

(s/defschema MetadataSaveRequest
  {:dest
   (describe NonBlankString "An IRODS path to a destination file where the metadata will be saved")

   :recursive
   (describe Boolean
     "When set to true and the given source is a folder, then all files and subfolders (plus all
      their files and subfolders) under the source folder will be included in the exported file,
      along with all of their metadata")})

(s/defschema RenameResult
  {:user
   (describe NonBlankString "The user performing the request.")

   :source
   (describe NonBlankString "An iRODS path to the initial location of the data item being renamed.")

   :dest
   (describe NonBlankString "An iRODS path to the destination of the data item being renamed.")})

(s/defschema MultiRenameResult
  {:user
   (describe NonBlankString "The user performing the request.")

   :sources
   (describe [NonBlankString] "iRODS paths to the initial locations of the data items being renamed.")

   :dest
   (describe NonBlankString "An iRODS path to the destination directory of the data items being renamed.")})

(s/defschema Filename
  {:filename (describe NonBlankString "The name of the data item.")})

(s/defschema Dirname
  {:dirname (describe NonBlankString "The directory name of the data item.")})

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

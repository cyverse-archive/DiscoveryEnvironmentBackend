(ns data-info.routes.domain.stats
  (:use [compojure.api.sweet :only [describe]]
        [data-info.routes.domain.common])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(def DataTypeEnum (s/enum :file :dir))
(def PermissionEnum (s/enum :read :write :own))
(def DataItemIdParam (describe UUID "The UUID of this data item"))
(def DataItemPathParam (describe NonBlankString "The IRODS paths to this data item"))

(s/defschema PathIdInfo
  {:id   DataItemIdParam
   :path DataItemPathParam})

(s/defschema DataStatInfo
  {:id
   DataItemIdParam

   :path
   DataItemPathParam

   :type
   (describe DataTypeEnum "The data item's type")

   :date-created
   (describe Long "The date this data item was created")

   :date-modified
   (describe Long "The date this data item was last modified")

   :permission
   (describe PermissionEnum "The requesting user's permissions on this data item")

   (s/optional-key :share-count)
   (describe Long
     "The number of other users this data item is shared with (only displayed to users with 'own'
     permissions)")})

(s/defschema DirStatInfo
  (merge DataStatInfo
    {:file-count (describe Long "The number of files under this directory")
     :dir-count  (describe Long "The number of subdirectories under this directory")}))

(s/defschema FileStatInfo
  (merge DataStatInfo
    {:file-size
     (describe Long "The size in bytes of this file")

     :content-type
     (describe NonBlankString "The detected media type of the data contained in this file")

     :infoType
     (describe String "The type of contents in this file")

     :md5
     (describe String "The md5 hash of this file's contents, as calculated and saved by IRODS")}))

(s/defschema FileStat
  {:file (describe FileStatInfo "File info")})

(s/defschema PathsMap
  {(describe s/Keyword "The IRDOS data item's path")
   (describe (s/either FileStatInfo DirStatInfo) "The data item's info")})

(s/defschema StatusInfo
  {:paths (describe PathsMap "Paths info")})

;; Used only for display as documentation in Swagger UI
(s/defschema StatResponsePathsMap
  {:/path/from/request/to/a/folder (describe DirStatInfo "A folder's info")
   :/path/from/request/to/a/file   (describe FileStatInfo "A file's info")})

;; Used only for display as documentation in Swagger UI
(s/defschema StatResponse
  {:paths (describe StatResponsePathsMap "A map of paths from the request to their status info")})

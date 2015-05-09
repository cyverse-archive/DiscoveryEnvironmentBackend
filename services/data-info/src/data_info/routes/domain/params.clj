(ns data-info.routes.domain.params
  (:use [compojure.api.sweet :only [describe]])
  (:require [schema.core :as s])
  (:import [java.util UUID]))

(def DataIdPathParam (describe UUID "The data items's UUID"))
(def DataTypeEnum (s/enum :file :dir))
(def PermissionEnum (s/enum :read :write :own))

(s/defschema SecuredQueryParamsRequired
  {:user (describe String "The IRODS username of the requesting user")})

(s/defschema Paths
  {:paths (describe [String] "A list of IRODS paths")})

(s/defschema DataStatInfo
  {:id
   (describe UUID "The UUID of this data item")

   :path
   (describe String "The IRODS paths to this data item")

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
     (describe String "The detected media type of the data contained in this file")

     :infoType
     (describe String "The type of contents in this file")

     :md5
     (describe String
       "The md5 hash of this file's contents, as calculated and saved by IRODS")}))

(s/defschema FileStat
  {:file (describe FileStatInfo "File info")})

(s/defschema PathsMap
  {(describe s/Keyword "The IRDOS data item's path")
   (describe (s/either FileStatInfo DirStatInfo) "The data item's info")})

(s/defschema StatusInfo
  {:paths (describe PathsMap "Paths info")})

(s/defschema MetadataSaveRequest
  {:dest
   (describe String "An IRODS path to a destination file where the metadata will be saved")

   :recursive
   (describe Boolean
     "When set to true and the given source is a folder, then all files and subfolders (plus all
      their files and subfolders) under the source folder will be included in the exported file,
      along with all of their metadata")})

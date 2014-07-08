(ns dewey.doc-prep
  "This is the index entry preparation logic."
  (:require [clj-jargon.permissions :as irods]
            [clojure-commons.file-utils :as file])
  (:import [java.util Date]))


(defn- format-user
  ([user] (format-user (:name user) (:zone user)))
  ([name zone] (str name \# zone)))


(defn- format-acl-entry
  [acl-entry]
  {:permission (irods/fmt-perm (.getFilePermissionEnum acl-entry))
   :user       (format-user (.getUserName acl-entry) (.getUserZone acl-entry))})


(defn format-acl
  "Formats an ACL entry for indexing.

   Parameters:
     acl - This is a list of UserFilePermission objects.

   Returns:
   The result is an list of maps. Each map indicates a user permission and has the following form.

   {:permission :own|:read|:write
    :user       name#zone}"
  [acl]
  (remove (comp nil? :permission) (map format-acl-entry acl)))


(defmulti
  ^{:doc "Formats a time for indexing. The resulting form will be in milliseconds since epoch."}
  format-time type)

(defmethod format-time String
  [posix-time-ms]
  (Long/parseLong posix-time-ms))

(defmethod format-time Date
  [time]
  (.getTime time))


(defn format-metadata
  "Formats the AVU metadata for indexing. It accepts the metadata as a list of maps representing AVU
   triples of the form produced by the clj-jargon library. It returns a list of maps of the
   following form.

   {:attribute name-str
    :value     value-str
    :unit      unit-str|nil}"
  [metadata]
  (letfn [(format-avu [avu] {:attribute (:attr avu)
                             :value     (:value avu)
                             :unit      (:unit avu)})]
    (map format-avu metadata)))


(defn format-file
  "Formats a file entry for indexing.

   Parameters:
     path - The path to the file.
     acl - The file ACL in the form of a list of UserFilePermission objects.
     creator - The file's creator as map with :name and :zone keys.
     date-created - The time when the file was created as a String or Date object.
     date-modified - The time when the file was last modified as a String or Date object.
     metadata - A list of AVU triples in the form produced by the clj-jargon library.
     file-size - The size of the file in bytes.
     file-type - The media type of the file."
  [path acl creator date-created date-modified metadata file-size file-type]
  {:id              path
   :path            path
   :label           (file/basename path)
   :userPermissions (format-acl acl)
   :creator         (format-user creator)
   :dateCreated     (format-time date-created)
   :dateModified    (format-time date-modified)
   :metadata        (format-metadata metadata)
   :fileSize        file-size
   :fileType        file-type})


(defn format-folder
  "Formats a folder entry for indexing.

   Parameters:
     path - The path to the folder.
     acl - The folder ACL in the form of a list of UserFilePermission objects.
     creator - The folder's creator as map with :name and :zone keys.
     date-created - The time when the folder was created as a String or Date object.
     date-modified - The time when the folder was last modified as a String or Date object.
     metadata - A list of AVU triples in the form produced by the clj-jargon library."
  [path acl creator date-created date-modified metadata]
  {:id              path
   :path            path
   :label           (file/basename path)
   :userPermissions (format-acl acl)
   :creator         (format-user creator)
   :dateCreated     (format-time date-created)
   :dateModified    (format-time date-modified)
   :metadata        (format-metadata metadata)})

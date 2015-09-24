(ns iplant_groups.routes.domain.privileges
  (:use [common-swagger-api.schema :only [describe]])
  (:require [iplant_groups.routes.domain.subject :as subject]
            [iplant_groups.routes.domain.group :as group]
            [iplant_groups.routes.domain.folder :as folder]
            [schema.core :as s]))

(s/defschema BasePrivilege
  {:type
   (describe String "The general type of privilege.")

   :name
   (describe String "The privilege name, under the type")

   :allowed
   (describe Boolean "Whether the privilege is marked allowed.")

   :revokable
   (describe Boolean "Whether the privilege is marked revokable.")

   :subject
   (describe subject/Subject "The subject/user with the privilege.")})

(s/defschema GroupPrivilege
  (assoc BasePrivilege
         :group (describe group/Group "The group the permission applies to.")))

(s/defschema FolderPrivilege
  (assoc BasePrivilege
         :folder (describe folder/Folder "The folder the permission applies to.")))

(s/defschema GroupPrivileges
  {:privileges (describe [GroupPrivilege] "A list of group-centric privileges")})

(s/defschema FolderPrivileges
  {:privileges (describe [FolderPrivilege] "A list of folder-centric privileges")})

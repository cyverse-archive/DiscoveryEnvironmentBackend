(ns data-info.routes.domain.users
  (:use [common-swagger-api.schema :only [describe]])
  (:require [schema.core :as s]))

(def PermissionEnum (s/enum :read :write :own))

(s/defschema UserPermission
  {:user (describe String "The user's short username")
   :permission (describe PermissionEnum "The user's level of permission")})

(s/defschema PermissionsEntry
  {:path (describe String "The iRODS path to this file.")
   :user-permissions (describe [UserPermission] "An array of objects describing permissions.")})

(s/defschema PermissionsResponse
  {:paths (describe [PermissionsEntry] "An array of objects describing files and their permissions")})

(ns data-info.routes.domain.exists
  (:use [common-swagger-api.schema :only [describe]])
  (:require [schema.core :as s]))

(s/defschema PathExistenceMap
  {(describe s/Keyword "The IRDOS data item's path")
   (describe Boolean "Whether this path from the request exists")})

(s/defschema ExistenceInfo
  {:paths
   (describe PathExistenceMap "Paths existence mapping")})

;; Used only for display as documentation in Swagger UI
(s/defschema ExistenceResponsePathsMap
  {:/path/from/request/to/a/file/or/folder
   (describe Boolean "Whether this folder from the request exists")})

;; Used only for display as documentation in Swagger UI
(s/defschema ExistenceResponse
  {:paths
   (describe ExistenceResponsePathsMap "A map of paths from the request to their existence info")})

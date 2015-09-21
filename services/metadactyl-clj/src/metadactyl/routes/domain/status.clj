(ns metadactyl.routes.domain.status
  (:use [common-swagger-api.schema :only [describe NonBlankString]])
  (:require [schema.core :as s]))

(s/defschema StatusResponse
  {:service     (describe NonBlankString "The name of the service")
   :description (describe NonBlankString "The service description")
   :version     (describe NonBlankString "The service version")
   :docs-url    (describe NonBlankString "The service API docs")})

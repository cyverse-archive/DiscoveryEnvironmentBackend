(ns metadata.routes.domain.status
  (:use [compojure.api.sweet :only [describe]]
        [metadata.routes.domain.common])
  (:require [schema.core :as s]))

(s/defschema StatusResponse
  {:service     (describe NonBlankString "The name of the service")
   :description (describe NonBlankString "The service description")
   :version     (describe NonBlankString "The service version")})

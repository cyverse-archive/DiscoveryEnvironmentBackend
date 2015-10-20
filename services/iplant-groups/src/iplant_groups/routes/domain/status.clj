(ns iplant_groups.routes.domain.status
  (:use [common-swagger-api.schema :only [describe NonBlankString]]
        [iplant_groups.routes.domain.params])
  (:require [schema.core :as s]))

(s/defschema StatusResponse
  {:service     (describe NonBlankString "The name of the service")
   :description (describe NonBlankString "The service description")
   :version     (describe NonBlankString "The service version")
   :docs-url    (describe NonBlankString "The service API docs")
   :grouper     (describe Boolean "Whether grouper is available and reports an acceptable state (with grouper diagnostic level of 'sources')")})

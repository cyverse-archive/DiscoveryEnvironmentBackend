(ns iplant_groups.routes.domain.status
  (:use [common-swagger-api.schema :only [describe NonBlankString StatusResponse]]
        [iplant_groups.routes.domain.params])
  (:require [schema.core :as s]))

(s/defschema IplantGroupsStatusResponse
  (assoc StatusResponse
         :grouper (describe Boolean "Whether grouper is available and reports an acceptable state (with grouper diagnostic level of 'sources')")))

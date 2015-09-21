(ns metadactyl.routes.status
  (:use [common-swagger-api.schema]
        [metadactyl.routes.domain.status])
  (:require [clojure-commons.service :as commons-service]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]))

(defroutes* status
  (GET* "/" [:as {:keys [uri server-name server-port]}]
    :return StatusResponse
    :summary "Service Information"
    :description "This endpoint provides the name of the service and its version."
    (service/trap uri
      commons-service/get-docs-status config/svc-info server-name server-port config/docs-uri)))

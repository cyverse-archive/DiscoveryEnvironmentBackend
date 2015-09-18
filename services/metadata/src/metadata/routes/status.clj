(ns metadata.routes.status
  (:use [common-swagger-api.schema]
        [metadata.routes.domain.status])
  (:require [clojure-commons.service :as commons-svc]
            [metadata.util.config :as config]
            [metadata.util.service :as service]))

(defroutes* status
  (context* "/" []
    :tags ["service-info"]

    (GET* "/" [:as {:keys [uri server-name server-port]}]
      :return StatusResponse
      :summary "Service Information"
      :description "This endpoint provides the name of the service and its version."
      (service/trap uri
        commons-svc/get-docs-status config/svc-info server-name server-port config/docs-uri))))

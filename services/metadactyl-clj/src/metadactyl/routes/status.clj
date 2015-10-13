(ns metadactyl.routes.status
  (:use [common-swagger-api.schema]
        [metadactyl.routes.domain.status]
        [ring.util.http-response :only [ok]])
  (:require [clojure-commons.service :as commons-service]
            [metadactyl.util.config :as config]))

(defroutes* status
  (GET* "/" [:as {:keys [server-name server-port]}]
    :return StatusResponse
    :summary "Service Information"
    :description "This endpoint provides the name of the service and its version."
    (ok
      (commons-service/get-docs-status config/svc-info server-name server-port config/docs-uri))))

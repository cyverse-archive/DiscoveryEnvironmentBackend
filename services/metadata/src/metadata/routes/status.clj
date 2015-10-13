(ns metadata.routes.status
  (:use [common-swagger-api.schema]
        [metadata.routes.domain.status]
        [ring.util.http-response :only [ok]])
  (:require [clojure-commons.service :as commons-svc]
            [metadata.util.config :as config]))

(defroutes* status
  (context* "/" []
    :tags ["service-info"]

    (GET* "/" [:as {:keys [server-name server-port]}]
      :return StatusResponse
      :summary "Service Information"
      :description "This endpoint provides the name of the service and its version."
      (ok
        (commons-svc/get-docs-status config/svc-info server-name server-port config/docs-uri)))))

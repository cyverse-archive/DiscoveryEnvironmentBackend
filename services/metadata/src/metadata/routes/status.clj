(ns metadata.routes.status
  (:use [compojure.api.sweet]
        [metadata.routes.domain.status])
  (:require [metadata.util.service :as service]))

(defroutes* status
  (context* "/status" []
    :tags ["service-info"]

    (GET* "/" [:as {uri :uri}]
      :return StatusResponse
      :summary "Service Information"
      :description "This endpoint provides the name of the service and its version."
      (service/trap uri service/get-status))))

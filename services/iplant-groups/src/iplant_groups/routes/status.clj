(ns iplant_groups.routes.status
  (:use [compojure.api.sweet]
        [iplant_groups.routes.domain.status])
  (:require [clojure-commons.service :as commons-service]
            [iplant_groups.util.config :as config]
            [iplant_groups.util.service :as service]))

(defroutes* status
  (GET* "/" [:as {:keys [uri server-name server-port]}]
        :return      StatusResponse
        :summary     "Service Information"
        :description "This endpoint provides the name of the service and its version."
        (service/trap uri commons-service/get-docs-status config/svc-info server-name server-port
                      config/docs-uri)))

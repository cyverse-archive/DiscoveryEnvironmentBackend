(ns iplant_groups.routes.status
  (:use [common-swagger-api.schema]
        [iplant_groups.routes.domain.status]
        [ring.util.http-response :only [ok]])
  (:require [clojure-commons.service :as commons-service]
            [iplant_groups.clients.grouper :as grouper]
            [iplant_groups.util.config :as config]))

(defroutes* status
  (GET* "/" [:as {:keys [uri server-name server-port]}]
        :return      IplantGroupsStatusResponse
        :summary     "Service Information"
        :description "This endpoint provides the name of the service and its version."
        (ok (assoc (commons-service/get-docs-status config/svc-info server-name server-port config/docs-uri)
                   :grouper (grouper/grouper-ok?)))))

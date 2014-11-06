(ns data-info.services.service-info
  (:require [liberator.core :refer [defresource]]
            [trptcolin.versioneer.core :as ver]
            [data-info.util.config :as cfg]))


(def ^:private resp
  {:service      (:app-name cfg/svc-info)
   :description  (:desc cfg/svc-info)
   :version      (ver/get-version (:group-id cfg/svc-info) (:art-id cfg/svc-info))})


(defresource service-info []
  :available-media-types ["application/json"]
  :handle-ok             resp)



(ns metadata.routes.templates
  (:use [compojure.api.sweet]
        [metadata.routes.domain.template])
  (:require [metadata.services.templates :as templates]
            [metadata.util.service :as service]))

(defroutes* templates
  (context* "/templates" []
    :tags ["metadata-template-info"]

    (GET* "/" [:as {uri :uri}]
      :return MetadataTemplates
      :summary "List Metadata Templates"
      :description "This endpoint lists all metadata templates."
      (service/trap uri templates/list-templates))))

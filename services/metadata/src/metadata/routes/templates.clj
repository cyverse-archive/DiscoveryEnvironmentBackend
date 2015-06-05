(ns metadata.routes.templates
  (:use [compojure.api.sweet]
        [metadata.routes.domain.common]
        [metadata.routes.domain.template])
  (:require [metadata.services.templates :as templates]
            [metadata.util.service :as service]))

(defroutes* templates
  (context* "/templates" []
    :tags ["template-info"]

    (GET* "/" [:as {:keys [uri]}]
      :query [params StandardQueryParams]
      :return MetadataTemplateList
      :summary "List Metadata Templates"
      :description "This endpoint lists undeleted metadata templates."
      (service/trap uri templates/list-templates))

    (GET* "/attr/:attr-id" [:as {:keys [uri]}]
      :path-params [attr-id :- AttrIdPathParam]
      :query [params StandardQueryParams]
      :return MetadataTemplateAttr
      :summary "View a Metadata Attribute"
      :description "This endpoint returns the details of a single metadata attribute."
      (service/trap uri templates/view-attribute attr-id))

    (GET* "/:template-id" [:as {:keys [uri]}]
      :path-params [template-id :- TemplateIdPathParam]
      :query [params StandardQueryParams]
      :return MetadataTemplate
      :summary "View a Metadata Template"
      :description "This endpoint returns the details of a single metadata template."
      (service/trap uri templates/view-template template-id))))

(defroutes* admin-templates
  (context* "/admin/templates" []
    :tags ["template-administration"]

    (GET* "/" [:as {:keys [uri]}]
      :query [params StandardQueryParams]
      :return MetadataTemplateList
      :summary "List Metadata Templates for Administrators"
      :description "This endpoint lists all metadata templates."
      (service/trap uri templates/admin-list-templates))))

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
      (service/trap uri templates/admin-list-templates))

    (POST* "/" [:as {:keys [uri]}]
      :query [params StandardQueryParams]
      :body [body (describe MetadataTemplateUpdate "The template to add.")]
      :return MetadataTemplate
      :summary "Add a Metadata Template"
      :description "This endpoint allows administrators to add new metadata templates."
      (service/trap uri templates/add-template params body))

    (PUT* "/:template-id" [:as {:keys [uri]}]
      :path-params [template-id :- TemplateIdPathParam]
      :body [body (describe MetadataTemplateUpdate "The template to update.")]
      :query [params StandardQueryParams]
      :return MetadataTemplate
      :summary "Update a Metadata Template"
      :description "This endpoint allows administrators to update existing metadata templates."
      (service/trap uri templates/update-template params template-id body))

    (DELETE* "/:template-id" [:as {:keys [uri]}]
      :path-params [template-id :- TemplateIdPathParam]
      :query [params StandardQueryParams]
      :summary "Mark a Metadata Template as Deleted"
      :description "This endpoint allows administrators to mark existing metadata templates as
      deleted."
      (service/trap uri templates/delete-template params template-id))))

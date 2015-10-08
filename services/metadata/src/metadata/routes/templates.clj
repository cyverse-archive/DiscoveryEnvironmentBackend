(ns metadata.routes.templates
  (:use [common-swagger-api.schema]
        [metadata.routes.domain.common]
        [metadata.routes.domain.template]
        [ring.util.http-response :only [ok]])
  (:require [metadata.services.templates :as templates]))

(defroutes* templates
  (context* "/templates" []
    :tags ["template-info"]

    (GET* "/" []
      :query [params StandardUserQueryParams]
      :return MetadataTemplateList
      :summary "List Metadata Templates"
      :description "This endpoint lists undeleted metadata templates."
      (ok (templates/list-templates)))

    (GET* "/attr/:attr-id" []
      :path-params [attr-id :- AttrIdPathParam]
      :query [params StandardUserQueryParams]
      :return MetadataTemplateAttr
      :summary "View a Metadata Attribute"
      :description "This endpoint returns the details of a single metadata attribute."
      (ok (templates/view-attribute attr-id)))

    (GET* "/:template-id" []
      :path-params [template-id :- TemplateIdPathParam]
      :query [params StandardUserQueryParams]
      :return MetadataTemplate
      :summary "View a Metadata Template"
      :description "This endpoint returns the details of a single metadata template."
      (ok (templates/view-template template-id)))))

(defroutes* admin-templates
  (context* "/admin/templates" []
    :tags ["template-administration"]

    (GET* "/" []
      :query [params StandardUserQueryParams]
      :return MetadataTemplateList
      :summary "List Metadata Templates for Administrators"
      :description "This endpoint lists all metadata templates."
      (ok (templates/admin-list-templates)))

    (POST* "/" []
      :query [params StandardUserQueryParams]
      :body [body (describe MetadataTemplateUpdate "The template to add.")]
      :return MetadataTemplate
      :summary "Add a Metadata Template"
      :description "This endpoint allows administrators to add new metadata templates."
      (ok (templates/add-template params body)))

    (PUT* "/:template-id" []
      :path-params [template-id :- TemplateIdPathParam]
      :body [body (describe MetadataTemplateUpdate "The template to update.")]
      :query [params StandardUserQueryParams]
      :return MetadataTemplate
      :summary "Update a Metadata Template"
      :description "This endpoint allows administrators to update existing metadata templates."
      (ok (templates/update-template params template-id body)))

    (DELETE* "/:template-id" []
      :path-params [template-id :- TemplateIdPathParam]
      :query [params StandardUserQueryParams]
      :summary "Mark a Metadata Template as Deleted"
      :description "This endpoint allows administrators to mark existing metadata templates as
      deleted."
      (ok (templates/delete-template params template-id)))))

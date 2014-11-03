(ns metadactyl.routes.admin
  (:use [metadactyl.app-categorization :only [categorize-apps]]
        [metadactyl.metadata.reference-genomes :only [delete-reference-genome
                                                      replace-reference-genomes]]
        [metadactyl.metadata.tool-requests]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.domain.app.category]
        [metadactyl.routes.domain.reference-genome]
        [metadactyl.routes.domain.tool]
        [metadactyl.routes.params]
        [metadactyl.service.app-metadata :only [delete-categories permanently-delete-apps]]
        [metadactyl.tools :only [add-tools]]
        [metadactyl.user :only [current-user]]
        [compojure.api.sweet]
        [ring.swagger.schema :only [describe]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]
            [compojure.route :as route])
  (:import [java.util UUID]))

(defroutes* tools
  (POST* "/tools" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe ToolsImportRequest "The Tools to import.")]
         :summary "Add new Tools."
         :notes "This service adds new Tools to the DE."
         (ce/trap uri #(add-tools body)))

  (GET* "/tool-requests" [:as {uri :uri}]
        :query [params ToolRequestListingParams]
        :return ToolRequestListing
        :summary "List Tool Requests"
        :notes "This endpoint lists high level details about tool requests that have been submitted.
        Administrators may use this endpoint to track tool requests for all users."
        (ce/trap uri #(list-tool-requests params)))

  (GET* "/tool-requests/:request-id" [:as {uri :uri}]
        :path-params [request-id :- ToolRequestIdParam]
        :query [params SecuredQueryParams]
        :return ToolRequestDetails
        :summary "Obtain Tool Request Details"
        :notes "This service obtains detailed information about a tool request. This is the service
        that the DE support team uses to obtain the request details."
        (ce/trap uri #(get-tool-request request-id)))

  (POST* "/tool-requests/:request-id/status" [:as {uri :uri}]
         :path-params [request-id :- ToolRequestIdParam]
         :query [params SecuredQueryParams]
         :body [body (describe ToolRequestStatusUpdate "A Tool Request status update.")]
         :return ToolRequestDetails
         :summary "Update the Status of a Tool Request"
         :notes "This endpoint is used by Discovery Environment administrators to update the status
         of a tool request."
         (ce/trap uri
           #(update-tool-request request-id (config/uid-domain) (:username current-user) body))))

(defroutes* admin-apps
  (POST* "/" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe AppCategorizationRequest "An App Categorization Request.")]
         :summary "Categorize Apps"
         :notes "This endpoint is used by the Admin interface to add or move Apps to into multiple
         Categories."
         (ce/trap uri #(categorize-apps body)))

  (POST* "/categories/shredder" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe AppCategoryIdList "A List of App Category IDs to delete.")]
         :return AppCategoryIdList
         :summary "Delete App Categories"
         :notes "App Categories can be deleted using this endpoint. The App Category and all of its
         subcategories will be deleted by this service, but no Apps will be removed. The response
         contains a list of Category IDs for which the deletion failed (including any subcategories
         of a Category already included in the request)."
         (ce/trap uri #(delete-categories body)))

  (POST* "/shredder" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe AppDeletionRequest "List of App IDs to delete.")]
         :summary "Permanently Deleting Apps"
         :notes "This service physically removes an App from the database, which allows
         administrators to completely remove Apps that are causing problems."
         (ce/trap uri #(permanently-delete-apps body))))

(defroutes* reference-genomes
  (PUT* "/" [:as {uri :uri}]
            :query [params SecuredQueryParams]
            :body [body (describe ReferenceGenomesSetRequest "List of Reference Genomes to set.")]
            :return ReferenceGenomesList
            :summary "Replace Reference Genomes."
            :notes "This endpoint replaces ALL the Reference Genomes in the Discovery Environment,
            so if a genome is not listed in the request, it will not show up in the DE."
            (ce/trap uri #(replace-reference-genomes body)))

  (DELETE* "/:reference-genome-id" [:as {uri :uri}]
           :path-params [reference-genome-id :- ReferenceGenomeIdParam]
           :query [params SecuredQueryParams]
           :summary "Delete a Reference Genome."
           :notes "A Reference Genome can be marked as deleted in the DE without being completely
           removed from the database using this service. <b>Note</b>: an attempt to delete a
           Reference Genome that is already marked as deleted is treated as a no-op rather than an
           error condition. If the Reference Genome doesn't exist in the database at all, however,
           then that is treated as an error condition."
           (ce/trap uri #(delete-reference-genome reference-genome-id))))

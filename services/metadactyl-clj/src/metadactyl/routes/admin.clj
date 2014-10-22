(ns metadactyl.routes.admin
  (:use [metadactyl.app-categorization :only [categorize-apps]]
        [metadactyl.metadata.tool-requests]
        [metadactyl.routes.domain.app]
        [metadactyl.routes.domain.app.category]
        [metadactyl.routes.domain.tool-requests]
        [metadactyl.routes.params]
        [metadactyl.service.app-metadata :only [permanently-delete-apps]]
        [metadactyl.user :only [current-user]]
        [compojure.api.sweet]
        [ring.swagger.schema :only [describe]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]
            [compojure.route :as route])
  (:import [java.util UUID]))

(defroutes* tool-requests
  (GET* "/" []
        :query [params ToolRequestListingParams]
        :return ToolRequestListing
        :summary "List Tool Requests"
        :notes "This endpoint lists high level details about tool requests that have been submitted.
        Administrators may use this endpoint to track tool requests for all users."
        (service/trap #(list-tool-requests params)))

  (GET* "/:request-id" []
        :path-params [request-id :- ToolRequestIdParam]
        :query [params SecuredQueryParams]
        :return ToolRequestDetails
        :summary "Obtain Tool Request Details"
        :notes "This service obtains detailed information about a tool request. This is the service
        that the DE support team uses to obtain the request details."
        (service/trap #(get-tool-request request-id)))

  (POST* "/:request-id/status" []
         :path-params [request-id :- ToolRequestIdParam]
         :query [params SecuredQueryParams]
         :body [body (describe ToolRequestStatusUpdate "A Tool Request status update.")]
         :return ToolRequestDetails
         :summary "Update the Status of a Tool Request"
         :notes "This endpoint is used by Discovery Environment administrators to update the status
         of a tool request."
         (service/trap
           #(update-tool-request request-id (config/uid-domain) (:username current-user) body)))

  (route/not-found (service/unrecognized-path-response)))

(defroutes* admin-apps
  (POST* "/" [:as {body :body}]
         :query [params SecuredQueryParams]
         :body [body (describe AppCategorizationRequest "An App Categorization Request.")]
         :summary "Categorize Apps"
         :notes "This endpoint is used by the Admin interface to add or move Apps to into multiple
         Categories."
         (service/trap #(categorize-apps body)))

  (POST* "/shredder" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe AppDeletionRequest "List of App IDs to delete.")]
         :summary "Permanently Deleting Apps"
         :notes "This service physically removes an App from the database, which allows
         administrators to completely remove Apps that are causing problems."
         (ce/trap uri #(permanently-delete-apps body)))

  (route/not-found (service/unrecognized-path-response)))

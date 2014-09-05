(ns metadactyl.routes.tool-requests
  (:use [metadactyl.metadata.tool-requests]
        [metadactyl.routes.domain.tool-requests]
        [metadactyl.routes.params]
        [metadactyl.user :only [current-user]]
        [compojure.api.sweet]
        [ring.swagger.schema :only [describe]])
  (:require [metadactyl.util.service :as service]
            [compojure.route :as route]))

(defroutes* tool-requests
  (GET* "/" []
        :query [params ToolRequestListingParams]
        :return ToolRequestListing
        :summary "List Tool Requests"
        :notes "This endpoint lists high level details about tool requests that have been submitted.
        A user may track their own tool requests with this endpoint."
        (service/trap #(list-tool-requests (assoc params :username (:username current-user)))))

  (POST* "/" []
         :query [params SecuredQueryParams]
         :body [body (describe ToolRequest
                       "A tool installation request. One of `source_url` or `source_upload_file`
                        fields are required, but not both.")]
         :return ToolRequestDetails
         :summary "Request Tool Installation"
         :notes "This service submits a request for a tool to be installed so that it can be used
         from within the Discovery Environment. The installation request and all status updates
         related to the tool request will be tracked in the Discovery Environment database."
         (service/trap #(submit-tool-request (:username current-user) body)))

  (GET* "/status-codes" []
        :query [params StatusCodeListingParams]
        :summary "List Tool Request Status Codes"
        :return StatusCodeListing
        :notes "Tool request status codes are largely arbitrary, but once they've been used once,
        they're stored in the database so that they can be reused easily. This endpoint allows the
        caller to list the known status codes."
        (service/trap #(list-tool-request-status-codes params)))

  (route/not-found (service/unrecognized-path-response)))

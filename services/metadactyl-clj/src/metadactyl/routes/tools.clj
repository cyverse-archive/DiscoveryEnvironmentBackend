(ns metadactyl.routes.tools
  (:use [metadactyl.metadata.tool-requests]
        [metadactyl.routes.domain.tool]
        [metadactyl.routes.params]
        [metadactyl.tools :only [search-tools]]
        [metadactyl.user :only [current-user]]
        [compojure.api.sweet]
        [ring.swagger.schema :only [describe]])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.util.service :as service]
            [compojure.route :as route]))

(defroutes* tools
  (GET* "/tools" [:as {uri :uri}]
        :query [params ToolSearchParams]
        :return ToolListing
        :summary "Search Tools"
        :notes "This endpoint allows users to search for a tool with a name or description that
        contains the given search term."
        (ce/trap uri #(search-tools params)))

  (GET* "/tool-requests" [:as {uri :uri}]
        :query [params ToolRequestListingParams]
        :return ToolRequestListing
        :summary "List Tool Requests"
        :notes "This endpoint lists high level details about tool requests that have been submitted.
        A user may track their own tool requests with this endpoint."
        (ce/trap uri #(list-tool-requests (assoc params :username (:username current-user)))))

  (POST* "/tool-requests" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe ToolRequest
                       "A tool installation request. One of `source_url` or `source_upload_file`
                        fields are required, but not both.")]
         :return ToolRequestDetails
         :summary "Request Tool Installation"
         :notes "This service submits a request for a tool to be installed so that it can be used
         from within the Discovery Environment. The installation request and all status updates
         related to the tool request will be tracked in the Discovery Environment database."
         (ce/trap uri #(submit-tool-request (:username current-user) body)))

  (GET* "/tool-requests/status-codes" [:as {uri :uri}]
        :query [params StatusCodeListingParams]
        :summary "List Tool Request Status Codes"
        :return StatusCodeListing
        :notes "Tool request status codes are largely arbitrary, but once they've been used once,
        they're stored in the database so that they can be reused easily. This endpoint allows the
        caller to list the known status codes."
        (ce/trap uri #(list-tool-request-status-codes params))))

(ns metadactyl.routes.tools
  (:use [metadactyl.metadata.tool-requests]
        [metadactyl.containers]
        [metadactyl.schema.containers]
        [metadactyl.routes.domain.tool]
        [metadactyl.routes.params]
        [metadactyl.tools :only [get-tool search-tools]]
        [metadactyl.user :only [current-user]]
        [compojure.api.sweet]
        [ring.swagger.schema :only [describe]]
        [metadactyl.util.service])
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

  (GET* "/tools/:tool-id" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return Tool
        :summary "Get a Tool"
        :notes "This endpoint returns the details for one tool."
        (ce/trap uri #(get-tool tool-id)))

  (GET* "/tools/:tool-id/container" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return ToolContainer
        :summary "Tool Container Information"
        :notes "This endpoint returns container information associated with a tool. This endpoint
        returns a 404 if the tool is not run inside a container."
        (ce/trap uri #(let [retval (tool-container-info tool-id)]
                        (if (nil? retval)
                          (not-found-response (str "A container for " tool-id " was not found."))
                          (success-response retval)))))

  (GET* "/tools/:tool-id/container/devices" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return Devices
        :summary "Tool Container Device Information"
        :notes "Returns device information for the container associated with a tool."
        (ce/trap uri #(let [retval (tool-device-info tool-id)]
                        (if (nil? retval)
                          (not-found-response (str "A container for " tool-id " was not found."))
                          (success-response retval)))))

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

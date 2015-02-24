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

(defmacro requester
  "Handles calling functions and returning request maps. The body of the call
   must return a nil if any of the objects can't be found, otherwise it returns a map."
  [tool-id & funccall]
  `(fn []
     (let [retval# ~@funccall]
       (if (nil? retval#)
         (not-found-response (str "A container for " ~tool-id " was not found."))
         (success-response retval#)))))

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
        (ce/trap uri (requester tool-id (tool-container-info tool-id))))

  (GET* "/tools/:tool-id/container/devices" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return Devices
        :summary "Tool Container Device Information"
        :notes "Returns device information for the container associated with a tool."
        (ce/trap uri (requester tool-id (tool-device-info tool-id))))

  (POST* "/tools/:tool-id/container/devices" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body NewDevice]
         :return Device
         :summary "Adds Device To Tool Container"
         :notes "Adds a new device to a tool container."
         (ce/trap uri (requester tool-id (add-tool-device tool-id body))))

  (GET* "/tools/:tool-id/container/devices/:device-id" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam,
                      device-id :- DeviceIdParam]
        :query [params SecuredQueryParams]
        :return Device
        :summary "Tool Container Device Information"
        :notes "Returns device information for the container associated with a tool."
        (ce/trap uri (requester tool-id (tool-device tool-id device-id))))
  
  (GET* "/tools/:tool-id/container/devices/:device-id/host-path" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam device-id :- DeviceIdParam]
        :query [params SecuredQueryParams]
        :return DeviceHostPath
        :summary "Tool Container Device Host Path"
        :notes "Returns a device's host path."
        (ce/trap uri (requester tool-id (device-field tool-id device-id :host_path))))

  (POST* "/tools/:tool-id/container/devices/:device-id/host-path" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam device-id :- DeviceIdParam]
         :query [params SecuredQueryParams]
         :body [body DeviceHostPath]
         :return DeviceHostPath
         :summary "Update Tool Container Device Host Path"
         :notes "This endpoint updates a device's host path for the tool's container."
         (ce/trap uri (requester tool-id (update-device-field tool-id device-id :host_path (:host_path body)))))

  (GET* "/tools/:tool-id/container/devices/:device-id/container-path" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam device-id :- DeviceIdParam]
        :query [params SecuredQueryParams]
        :return DeviceContainerPath
        :summary "Tool Device Container Path"
        :notes "Returns a device's host path."
        (ce/trap uri (requester tool-id (device-field tool-id device-id :container_path))))
  
  (POST* "/tools/:tool-id/container/devices/:device-id/container-path" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam device-id :- DeviceIdParam]
         :query [params SecuredQueryParams]
         :body [body DeviceContainerPath]
         :return DeviceContainerPath
         :summary "Update Tool Device Container Path"
         :notes "This endpoint updates a device's host path for the tool's container."
         (ce/trap uri (requester tool-id (update-device-field tool-id device-id :container_path (:container_path body)))))
  
  (GET* "/tools/:tool-id/container/cpu-shares" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return CPUShares
        :summary "Tool Container CPU Shares"
        :notes "Returns the number of shares of the CPU that the tool container will receive."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :cpu_shares))))

  (POST* "/tools/:tool-id/container/cpu-shares" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body CPUShares]
         :return CPUShares
         :summary "Update Tool Container CPU Shares"
         :notes "This endpoint updates a the CPU shares for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :cpu_shares (:cpu_shares body)))))

  (GET* "/tools/:tool-id/container/memory-limit" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return MemoryLimit
        :summary "Tool Container Memory Limit"
        :notes "Returns the maximum amount of RAM that can be allocated to the tool container (in bytes)."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :memory_limit))))

  (POST* "/tools/:tool-id/container/memory-limit" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body MemoryLimit]
         :return MemoryLimit
         :summary "Update Tool Container Memory Limit"
         :notes "This endpoint updates a the memory limit for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :memory_limit (:memory_limit body)))))
  
  (GET* "/tools/:tool-id/container/network-mode" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return NetworkMode
        :summary "Tool Container Network Mode"
        :notes "Returns the network mode the tool container will operate in. Usually 'bridge' or 'none'."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :network_mode))))

  (POST* "/tools/:tool-id/container/network-mode" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body NetworkMode]
         :return NetworkMode
         :summary "Update Tool Container Network Mode"
         :notes "This endpoint updates a the network mode for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :network_mode (:network_mode body)))))
  
  (GET* "/tools/:tool-id/container/working-directory" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return WorkingDirectory
        :summary "Tool Container Working Directory"
        :notes "Sets the initial working directory for the tool container."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :working_directory))))

  (POST* "/tools/:tool-id/container/working-directory" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body WorkingDirectory]
         :return WorkingDirectory
         :summary "Update Tool Container Working Directory"
         :notes "This endpoint updates the working directory for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :working_directory (:working_directory body)))))
  
  (GET* "/tools/:tool-id/container/name" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return ContainerName
        :summary "Tool Container Name"
        :notes "The user supplied name that the container will be assigned when it runs."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :name))))
  
  (POST* "/tools/:tool-id/container/name" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body ContainerName]
         :return ContainerName
         :summary "Update Tool Container Name"
         :notes "This endpoint updates the container name for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :name (:name body)))))

  (GET* "/tools/:tool-id/container/volumes" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return Volumes
        :summary "Tool Container Volume Information"
        :notes "Returns volume information for the container associated with a tool."
        (ce/trap uri (requester tool-id (tool-volume-info tool-id))))

  (GET* "/tools/:tool-id/container/volumes/:volume-id" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam volume-id :- VolumeIdParam]
        :query [params SecuredQueryParams]
        :return Volume
        :summary "Tool Container Volume Information"
        :notes "Returns volume information for the container associated with a tool."
        (ce/trap uri (requester tool-id (tool-volume tool-id volume-id))))

  (GET* "/tools/:tool-id/container/volumes-from" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return VolumesFromList
        :summary "Tool Container Volumes From Information"
        :notes "Returns a list of container names that the container associated with the tool should import volumes from."
        (ce/trap uri (requester tool-id (tool-volumes-from-info tool-id))))

  (GET* "/tools/:tool-id/container/volumes-from/:volumes-from-id" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam volumes-from-id :- VolumesFromIdParam]
        :query [params SecuredQueryParams]
        :return VolumesFrom
        :summary "Tool Container Volumes From Information"
        :notes "Returns a list of container names that the container associated with the tool should import volumes from."
        (ce/trap uri (requester tool-id (tool-volumes-from tool-id volumes-from-id))))

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

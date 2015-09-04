(ns metadactyl.routes.tools
  (:use [metadactyl.metadata.tool-requests]
        [metadactyl.containers]
        [metadactyl.schema.containers]
        [metadactyl.routes.domain.tool]
        [metadactyl.routes.params]
        [metadactyl.tools :only [add-tools get-tool search-tools update-tool]]
        [metadactyl.user :only [current-user]]
        [compojure.api.sweet]
        [metadactyl.util.service])
  (:require [clojure-commons.error-codes :as ce]
            [metadactyl.util.service :as service]
            [compojure.route :as route]))

(def volumes-from-warning
  (str "\n\n#### Warning\n"
       "    Do not add `volumes-from` settings to tools unless it is certain that tool is authorized"
       " to access those data containers."))

(defmacro requester
  "Handles calling functions and returning request maps. The body of the call
   must return a nil if any of the objects can't be found, otherwise it returns a map."
  [tool-id & funccall]
  `(fn []
     (let [retval# ~@funccall]
       (if (nil? retval#)
         (not-found-response (str "A container for " ~tool-id " was not found."))
         (success-response retval#)))))

(defroutes* container-images
  (GET* "/" [:as {uri :uri}]
        :query [params SecuredQueryParams]
        :return Images
        :summary "List Container Images"
        :description "Returns all of the container images defined in the database."
        (service/trap uri list-images))

  (GET* "/:image-id" [:as {uri :uri}]
        :path-params [image-id :- ImageId]
        :query [params SecuredQueryParams]
        :return Image
        :summary "Container Image"
        :description "Returns a JSON description of a container image."
        (service/trap uri image-info image-id))

  (POST* "/" [:as {uri :uri}]
        :query [params SecuredQueryParams]
        :body [body NewImage]
        :return Image
        :summary "Add Container Image"
        :description "Adds a new container image to the system."
        (service/trap uri add-image-info body))

  (POST* "/:image-id/name" [:as {uri :uri}]
        :path-params [image-id :- ImageId]
        :query [params SecuredQueryParams]
        :body [body ImageName]
        :return Image
        :summary "Update Container Image Name"
        :description "Updates a container image's name field."
        (service/trap uri modify-image-info image-id body))

  (POST* "/:image-id/url" [:as {uri :uri}]
        :path-params [image-id :- ImageId]
        :query [params SecuredQueryParams]
        :body [body ImageURL]
        :return Image
        :summary "Update Container Image URL"
        :description "Updates a container image's url field."
        (service/trap uri modify-image-info image-id body))

  (POST* "/:image-id/tag" [:as {uri :uri}]
        :path-params [image-id :- ImageId]
        :query [params SecuredQueryParams]
        :body [body ImageTag]
        :return Image
        :summary "Update Container Image Tag"
        :description "Updates a container image's tag field."
        (service/trap uri modify-image-info image-id body))

  (DELETE* "/:image-id" [:as {uri :uri}]
        :path-params [image-id :- ImageId]
        :query [params SecuredQueryParams]
        :return nil
        :summary "Delete Container Image"
        :description "Deletes a container image from the system."
        (service/trap uri delete-image image-id)))

 (defroutes* data-containers
   (GET* "/" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :return DataContainers
         :summary "List Data Containers"
         :description "Lists all of the available data containers."
         (service/trap uri list-data-containers))

   (GET* "/:data-container-id" [:as {uri :uri}]
         :path-params [data-container-id :- DataContainerIdParam]
         :query [params SecuredQueryParams]
         :return DataContainer
         :summary "Data Container"
         :description "Returns a JSON description of a data container."
         (service/trap uri data-container data-container-id)))

(defroutes* admin-data-containers
  (PATCH* "/:data-container-id" [:as {uri :uri}]
          :path-params [data-container-id :- DataContainerIdParam]
          :query [params SecuredQueryParams]
          :body [body DataContainerUpdateRequest]
          :return DataContainer
          :summary "Update Data Container"
          :description "Updates a data container's settings."
          (service/trap uri modify-data-container data-container-id body)))

(defroutes* tools
  (GET* "/" [:as {uri :uri}]
        :query [params ToolSearchParams]
        :return ToolListing
        :summary "Search Tools"
        :description "This endpoint allows users to search for a tool with a name or description that
        contains the given search term."
        (ce/trap uri #(search-tools params)))

  (GET* "/:tool-id" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return Tool
        :summary "Get a Tool"
        :description "This endpoint returns the details for one tool."
        (ce/trap uri #(get-tool tool-id)))

  (GET* "/:tool-id/container" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return ToolContainer
        :summary "Tool Container Information"
        :description "This endpoint returns container information associated with a tool. This endpoint
        returns a 404 if the tool is not run inside a container."
        (ce/trap uri (requester tool-id (tool-container-info tool-id))))

  (GET* "/:tool-id/container/devices" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return Devices
        :summary "Tool Container Device Information"
        :description "Returns device information for the container associated with a tool."
        (ce/trap uri (requester tool-id (tool-device-info tool-id))))

  (GET* "/:tool-id/container/devices/:device-id" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam,
                      device-id :- DeviceIdParam]
        :query [params SecuredQueryParams]
        :return Device
        :summary "Tool Container Device Information"
        :description "Returns device information for the container associated with a tool."
        (ce/trap uri (requester tool-id (tool-device tool-id device-id))))

  (GET* "/:tool-id/container/devices/:device-id/host-path" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam device-id :- DeviceIdParam]
        :query [params SecuredQueryParams]
        :return DeviceHostPath
        :summary "Tool Container Device Host Path"
        :description "Returns a device's host path."
        (ce/trap uri (requester tool-id (device-field tool-id device-id :host_path))))

  (GET* "/:tool-id/container/devices/:device-id/container-path" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam device-id :- DeviceIdParam]
        :query [params SecuredQueryParams]
        :return DeviceContainerPath
        :summary "Tool Device Container Path"
        :description "Returns a device's in-container path."
        (ce/trap uri (requester tool-id (device-field tool-id device-id :container_path))))

  (GET* "/:tool-id/container/cpu-shares" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return CPUShares
        :summary "Tool Container CPU Shares"
        :description "Returns the number of shares of the CPU that the tool container will receive."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :cpu_shares))))

  (GET* "/:tool-id/container/memory-limit" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return MemoryLimit
        :summary "Tool Container Memory Limit"
        :description "Returns the maximum amount of RAM that can be allocated to the tool container (in bytes)."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :memory_limit))))

  (GET* "/:tool-id/container/network-mode" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return NetworkMode
        :summary "Tool Container Network Mode"
        :description "Returns the network mode the tool container will operate in. Usually 'bridge' or 'none'."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :network_mode))))

  (GET* "/:tool-id/container/working-directory" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return WorkingDirectory
        :summary "Tool Container Working Directory"
        :description "Sets the initial working directory for the tool container."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :working_directory))))

  (GET* "/:tool-id/container/entrypoint" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return Entrypoint
        :summary "Tool Container Entrypoint"
        :description "Get the entrypoint setting for the tool container."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :entrypoint))))

  (GET* "/:tool-id/container/name" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return ContainerName
        :summary "Tool Container Name"
        :description "The user supplied name that the container will be assigned when it runs."
        (ce/trap uri (requester tool-id (get-settings-field tool-id :name))))

  (GET* "/:tool-id/container/volumes" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return Volumes
        :summary "Tool Container Volume Information"
        :description "Returns volume information for the container associated with a tool."
        (ce/trap uri (requester tool-id (tool-volume-info tool-id))))

  (GET* "/:tool-id/container/volumes/:volume-id" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam volume-id :- VolumeIdParam]
        :query [params SecuredQueryParams]
        :return Volume
        :summary "Tool Container Volume Information"
        :description "Returns volume information for the container associated with a tool."
        (ce/trap uri (requester tool-id (tool-volume tool-id volume-id))))

  (GET* "/:tool-id/container/volumes/:volume-id/host-path" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam volume-id :- VolumeIdParam]
        :query [params SecuredQueryParams]
        :return VolumeHostPath
        :summary "Tool Container Volume Host Path"
        :description "Returns volume host path for the container associated with a tool."
        (ce/trap uri (requester tool-id (volume-field tool-id volume-id :host_path))))

  (GET* "/:tool-id/container/volumes/:volume-id/container-path" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam volume-id :- VolumeIdParam]
        :query [params SecuredQueryParams]
        :return VolumeContainerPath
        :summary "Tool Container Volume Container Path"
        :description "Returns volume container path for the container associated with a tool."
        (ce/trap uri (requester tool-id (volume-field tool-id volume-id :container_path))))

  (GET* "/:tool-id/container/volumes-from" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam]
        :query [params SecuredQueryParams]
        :return VolumesFromList
        :summary "Tool Container Volumes From Information"
        :description "Returns a list of container names that the container associated with the tool should import volumes from."
        (ce/trap uri (requester tool-id (tool-volumes-from-info tool-id))))

  (GET* "/:tool-id/container/volumes-from/:volumes-from-id" [:as {uri :uri}]
        :path-params [tool-id :- ToolIdParam volumes-from-id :- VolumesFromIdParam]
        :query [params SecuredQueryParams]
        :return VolumesFrom
        :summary "Tool Container Volumes From Information"
        :description "Returns the data container settings for the given `volumes-from-id` the tool
         should import volumes from."
        (ce/trap uri (requester tool-id (tool-volumes-from tool-id volumes-from-id)))))

(defroutes* tool-requests
  (GET* "/" [:as {uri :uri}]
        :query [params ToolRequestListingParams]
        :return ToolRequestListing
        :summary "List Tool Requests"
        :description "This endpoint lists high level details about tool requests that have been submitted.
        A user may track their own tool requests with this endpoint."
        (service/trap uri list-tool-requests (assoc params :username (:username current-user))))

  (POST* "/" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe ToolRequest
                       "A tool installation request. One of `source_url` or `source_upload_file`
                        fields are required, but not both.")]
         :return ToolRequestDetails
         :summary "Request Tool Installation"
         :description "This service submits a request for a tool to be installed so that it can be used
         from within the Discovery Environment. The installation request and all status updates
         related to the tool request will be tracked in the Discovery Environment database."
         (service/trap uri submit-tool-request current-user body))

  (GET* "/status-codes" [:as {uri :uri}]
        :query [params StatusCodeListingParams]
        :summary "List Tool Request Status Codes"
        :return StatusCodeListing
        :description "Tool request status codes are largely arbitrary, but once they've been used once,
        they're stored in the database so that they can be reused easily. This endpoint allows the
        caller to list the known status codes."
        (service/trap uri list-tool-request-status-codes params)))

(defroutes* admin-tools
  (POST* "/" [:as {uri :uri}]
         :query [params SecuredQueryParams]
         :body [body (describe ToolsImportRequest "The Tools to import.")]
         :summary "Add new Tools."
         :description (str "This service adds new Tools to the DE." volumes-from-warning)
         (ce/trap uri #(add-tools body)))

  (PATCH* "/:tool-id" [:as {uri :uri}]
          :path-params [tool-id :- ToolIdParam]
          :query [params SecuredQueryParams]
          :body [body (describe ToolUpdateRequest "The Tool to update.")]
          :return Tool
          :summary "Update a Tool"
          :description "This service updates a Tool definition in the DE."
          (ce/trap uri #(update-tool (assoc body :id tool-id))))

  (POST* "/:tool-id/container/devices" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body NewDevice]
         :return Device
         :summary "Adds Device To Tool Container"
         :description "Adds a new device to a tool container."
         (ce/trap uri (requester tool-id (add-tool-device tool-id body))))

  (DELETE* "/:tool-id/container/devices/:device-id" [:as {uri :uri}]
           :path-params [tool-id :- ToolIdParam device-id :- DeviceIdParam]
           :query [params SecuredQueryParams]
           :return nil
           :summary "Delete a container device"
           :description "Deletes a device from the tool's container"
           (ce/trap uri #(delete-tool-device tool-id device-id)))

  (POST* "/:tool-id/container/devices/:device-id/host-path" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam device-id :- DeviceIdParam]
         :query [params SecuredQueryParams]
         :body [body DeviceHostPath]
         :return DeviceHostPath
         :summary "Update Tool Container Device Host Path"
         :description "This endpoint updates a device's host path for the tool's container."
         (ce/trap uri (requester tool-id (update-device-field tool-id device-id :host_path (:host_path body)))))

  (POST* "/:tool-id/container/devices/:device-id/container-path" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam device-id :- DeviceIdParam]
         :query [params SecuredQueryParams]
         :body [body DeviceContainerPath]
         :return DeviceContainerPath
         :summary "Update Tool Device Container Path"
         :description "This endpoint updates a device's container path for the tool's container."
         (ce/trap uri (requester tool-id (update-device-field tool-id device-id :container_path (:container_path body)))))

  (POST* "/:tool-id/container/entrypoint" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body Entrypoint]
         :return Entrypoint
         :summary "Update Tool Container Entrypoint"
         :description "This endpoint updates an entrypoint for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :entrypoint (:entrypoint body)))))

  (POST* "/:tool-id/container/cpu-shares" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body CPUShares]
         :return CPUShares
         :summary "Update Tool Container CPU Shares"
         :description "This endpoint updates a the CPU shares for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :cpu_shares (:cpu_shares body)))))

  (POST* "/:tool-id/container/memory-limit" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body MemoryLimit]
         :return MemoryLimit
         :summary "Update Tool Container Memory Limit"
         :description "This endpoint updates a the memory limit for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :memory_limit (:memory_limit body)))))

  (POST* "/:tool-id/container/network-mode" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body NetworkMode]
         :return NetworkMode
         :summary "Update Tool Container Network Mode"
         :description "This endpoint updates a the network mode for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :network_mode (:network_mode body)))))

  (POST* "/:tool-id/container/working-directory" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body WorkingDirectory]
         :return WorkingDirectory
         :summary "Update Tool Container Working Directory"
         :description "This endpoint updates the working directory for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :working_directory (:working_directory body)))))

  (POST* "/:tool-id/container/name" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body ContainerName]
         :return ContainerName
         :summary "Update Tool Container Name"
         :description "This endpoint updates the container name for the tool's container."
         (ce/trap uri (requester tool-id (update-settings-field tool-id :name (:name body)))))

  (POST* "/:tool-id/container/volumes" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body NewVolume]
         :return Volume
         :summary "Tool Container Volume Information"
         :description "This endpoint updates volume information for the container associated with a tool."
         (ce/trap uri (requester tool-id (add-tool-volume tool-id body))))

  (DELETE* "/:tool-id/container/volumes/:volume-id" [:as {uri :uri}]
           :path-params [tool-id :- ToolIdParam volume-id :- VolumeIdParam]
           :query [params SecuredQueryParams]
           :return nil
           :summary "Delete Tool Container Volume"
           :description "Deletes a volume from a tool container."
           (ce/trap uri #(delete-tool-volume tool-id volume-id)))

  (POST* "/:tool-id/container/volumes/:volume-id/host-path" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam volume-id :- VolumeIdParam]
         :query [params SecuredQueryParams]
         :body [body VolumeHostPath]
         :return VolumeHostPath
         :summary "Update Tool Container Volume Host Path"
         :description "This endpoint updates a volume host path for the tool's container."
         (ce/trap uri (requester tool-id (update-volume-field tool-id volume-id :host_path (:host_path body)))))

  (POST* "/:tool-id/container/volumes/:volume-id/container-path" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam volume-id :- VolumeIdParam]
         :query [params SecuredQueryParams]
         :body [body VolumeContainerPath]
         :return VolumeContainerPath
         :summary "Update Tool Container Volume Container Path"
         :description "This endpoint updates a volume container path for the tool's container."
         (ce/trap uri (requester tool-id (update-volume-field tool-id volume-id :container_path (:container_path body)))))

  (PUT* "/:tool-id/container/volumes-from" [:as {uri :uri}]
         :path-params [tool-id :- ToolIdParam]
         :query [params SecuredQueryParams]
         :body [body NewVolumesFrom]
         :return VolumesFrom
         :summary "Adds A Volume Host Container"
         :description (str
                        "Adds a new container from which the tool container will bind mount volumes."
                        volumes-from-warning)
         (ce/trap uri (requester tool-id (add-tool-volumes-from tool-id body))))

  (DELETE* "/:tool-id/container/volumes-from/:volumes-from-id" [:as {uri :uri}]
           :path-params [tool-id :- ToolIdParam volumes-from-id :- VolumesFromIdParam]
           :query [params SecuredQueryParams]
           :return nil
           :summary "Delete Tool Container Volumes From Information"
           :description "Deletes a container name that the tool container should import volumes from."
           (ce/trap uri #(delete-tool-volumes-from tool-id volumes-from-id))))

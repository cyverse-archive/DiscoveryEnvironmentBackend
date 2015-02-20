(ns metadactyl.schema.containers
  (:use [ring.swagger.schema :only [describe]]
        [metadactyl.routes.domain.tool :only [ToolIdParam]])
  (:require [schema.core :as s]))

(s/defschema Image
  (describe
   {:name                 s/Str
    :id                   s/Uuid
    (s/optional-key :tag) s/Str
    (s/optional-key :url) s/Str}
   "A map describing a container image."))
 
(s/defschema Settings
  (describe
   {:cpu_shares         Integer
    :memory_limit       Long
    :network_mode       s/Str
    :working_directory  s/Str
    :name               s/Str
    :id                 s/Uuid}
   "The group of settings for a container."))

(s/defschema Device
  (describe
   {:host_path      s/Str
    :container_path s/Str
    :id             s/Uuid}
   "Information about a device associated with a tool's container."))

(def DeviceIdParam
  (describe
   java.util.UUID
   "A device's UUID."))

(s/defschema Devices
  (describe
   {:container_devices [Device]}
   "A list of devices associated with a tool's container."))

(s/defschema Volume
  (describe
   {:host_path      s/Str
    :container_path s/Str
    :id             s/Uuid}
   "A map representing a bind mounted container volume."))

(def VolumeIdParam
  (describe
   java.util.UUID
   "A volume's UUID."))

(s/defschema Volumes
  (describe
   {:container_volumes [Volume]}
   "A list of Volumes associated with a tool's container."))

(s/defschema VolumesFrom
  (describe
   {:name s/Str
    :id   s/Uuid}
   "The name of a container from which to bind mount volumes."))

(s/defschema VolumesFromList
  (describe
   {:container_volumes_from [VolumesFrom]}
   "The list of VolumeFroms associated with a tool's container."))

(s/defschema ToolContainerSettings
  (describe
   (merge
    Settings
    {:container_devices      [Device]
     :container_volumes      [Volume]
     :container_volumes_from [VolumesFrom]})
   "Bare minimum map containing all of the container settings."))

(s/defschema ToolContainer
  (describe
   (merge
    ToolContainerSettings
    {:image Image})
   "All container and container image information associated with a tool."))

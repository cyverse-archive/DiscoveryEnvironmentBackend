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

(s/defschema CPUShares
  (describe
   {:cpu_shares Integer}
   "The shares of the CPU that the tool container will receive."))

(s/defschema MemoryLimit
  (describe
   {:memory_limit Long}
   "The amount of memory (in bytes) that the tool container is restricted to."))

(s/defschema NetworkMode
  (describe
   {:network_mode s/Str}
   "The network mode for the tool container."))

(s/defschema WorkingDirectory
  (describe
   {:working_directory s/Str}
   "The working directory in the tool container."))

(s/defschema ContainerName
  (describe
   {:name s/Str}
   "The name given to the tool container."))

(s/defschema Device
  (describe
   {:host_path      s/Str
    :container_path s/Str
    :id             s/Uuid}
   "Information about a device associated with a tool's container."))

(s/defschema NewDevice
  (describe
   (dissoc Device :id)
   "The map needed to add a device to a container."))

(s/defschema DeviceHostPath
  (describe
   {:host_path s/Str}
   "A device's path on the container host."))

(s/defschema DeviceContainerPath
  (describe
   {:container_path s/Str}
   "A device's path inside the tool container."))

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

(s/defschema VolumeHostPath
  (describe
   {:host_path s/Str}
   "The path to a bind mounted volume on the host machine."))

(s/defschema VolumeContainerPath
  (describe
   {:container_path s/Str}
   "The path to a bind mounted volume in the tool container."))

(s/defschema VolumesFrom
  (describe
   {:name s/Str
    :id   s/Uuid}
   "The name of a container from which to bind mount volumes."))

(s/defschema NewVolumesFrom
  (describe
   (dissoc VolumesFrom :id)
   "A map for adding a new container from which to bind mount volumes."))

(s/defschema VolumesFromName
  (describe
   {:name s/Str}
   "The name of a container from which volumes will be bind mounted."))

(def VolumesFromIdParam
  (describe
   java.util.UUID
   "A volume from's UUID."))

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

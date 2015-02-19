(ns metadactyl.schema.containers
  (:use [ring.swagger.schema :only [describe]]
        [metadactyl.routes.domain.tool :only [ToolIdParam]])
  (:require [schema.core :as s]))

(s/defschema ImageID (describe s/Uuid "The UUID for a container image. Primary key of the container_images table."))

(def ImageSpecifier
  (describe
   {:name                 s/Str
    (s/optional-key :tag) s/Str
    (s/optional-key :url) s/Str}
   "A map describing a container image."))

(s/defschema SettingsID
  (describe
   s/Uuid
   "The UUID for a group of container settings. Primary key of the container_settings table."))

(s/defschema Settings
  (describe
   {:cpu_shares   Integer
    :memory_limit Long
    :network_mode s/Str
    :working_dir  s/Str
    :name         s/Str
    :id           SettingsID}
   "The group of settings for a container."))

(s/defschema DeviceID
  (describe
   s/Uuid
   "The UUID for device associated with a group of container settings. Primary key of the container_devices table."))

(s/defschema DeviceHostPath
  (describe
   s/Str
   "The path to a device on the container host."))

(s/defschema DeviceContainerPath
  (describe
   s/Str
   "The path to a device within a container."))

(s/defschema Device
  (describe
   {:host_path DeviceHostPath
    :container_path DeviceContainerPath}
   "A map representing a Device."))

(s/defschema VolumeID
  (describe
   s/Uuid
   "The UUID for a volume associated with a group of container settings. Primary key of the container_volumes table."))

(s/defschema VolumeHostPath
  (describe
   s/Str
   "The path to a volume on the host that is shared with a container."))

(s/defschema VolumeContainerPath
  (describe
   s/Str
   "The path to a volume in a container that was bind mounted from the host."))

(s/defschema Volume
  (describe
   {:host_path VolumeHostPath
    :container_path VolumeContainerPath}
   "A map representing a bind mounted container volume."))

(s/defschema VolumesFromID
  (describe
   s/Uuid
   "The UUID for a 'volume from' setting associated with a group of container settings. Primary key of the container_volumes_from table."))

(s/defschema VolumesFromName
  (describe
   s/Str
   "The name of the container from which to mount volumes."))

(s/defschema VolumesFrom
  (describe
   {:name VolumesFromName}
   "The name of a container from which to bind mount volumes."))

(s/defschema ToolContainerSettings
  (describe
   (merge
    Settings
    {:container_devices [Device]
     :container_volumes [Volume]
     :container_volumes_from [VolumesFrom]})
   "Bare minimum map containing all of the container settings."))

(s/defschema ToolContainer
  {:cpu_shares Integer
   :memory_limit Long
   :network_mode String
   :working_directory String
   :name String
   :id java.util.UUID
   :container_devices
   [{:host_path String :container_path String}]
   :container_volumes
   [{:host_path String :container_path String}]
   :container_volumes_from
   [{:name String}]
   :image
   {:name String (s/optional-key :tag) String (s/optional-key :url) String}})

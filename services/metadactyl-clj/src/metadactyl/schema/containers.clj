(ns metadactyl.schema.containers
  (:use [ring.swagger.schema :only [describe]])
  (:require [schema.core :as s]))

(def ImageID (describe s/Uuid "The UUID for a container image. Primary key of the container_images table."))

(def ImageSpecifier
  (describe
   {:name s/Str
    (s/optional-key :tag) s/Str
    (s/optional-key :url) s/Str}
   "A map describing a container image."))

(def SettingsID
  (describe
   s/Uuid
   "The UUID for a group of container settings. Primary key of the container_settings table."))

(def Settings
  (describe
   {:cpu_shares   Integer
    :memory_limit Long
    :network_mode s/Str
    :working_dir  s/Str
    :name         s/Str}
   "The group of settings for a container."))

(def DeviceID
  (describe
   s/Uuid
   "The UUID for device associated with a group of container settings. Primary key of the container_devices table."))

(def DeviceHostPath
  (describe
   s/Str
   "The path to a device on the container host."))

(def DeviceContainerPath
  (describe
   s/Str
   "The path to a device within a container."))

(def VolumeID
  (describe
   s/Uuid
   "The UUID for a volume associated with a group of container settings. Primary key of the container_volumes table."))

(def VolumeHostPath
  (describe
   s/Uuid
   "The path to a volume on the host that is shared with a container."))

(def VolumeContainerPath
  (describe
   s/Uuid
   "The path to a volume in a container that was bind mounted from the host."))

(def VolumesFromID
  (describe
   s/Uuid
   "The UUID for a 'volume from' setting associated with a group of container settings. Primary key of the container_volumes_from table."))

(def VolumesFromName
  (describe
   s/Str
   "The name of the container from which to mount volumes."))

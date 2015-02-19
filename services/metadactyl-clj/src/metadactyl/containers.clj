(ns metadactyl.containers
  (:use [metadactyl.schema.containers]
        [metadactyl.routes.domain.tool :only [ToolIdParam]]
        [kameleon.core]
        [kameleon.entities :only [tools
                                  container-images
                                  container-settings
                                  container-devices
                                  container-volumes
                                  container-volumes-from]]
        [kameleon.uuids :only [uuidify]]
        [korma.core]
        [korma.db :only [transaction]])
  (:require [clojure.tools.logging :as log]
            [schema.core :as s]))

(defn containerized?
  "Returns true if the tool is available in a container."
  [tool-id]
  (pos?
   (count
    (select tools
            (fields :container_images_id)
            (where
             (and
              (= :id (uuidify tool-id))
              (not= :container_images_id nil)))))))

(defn image-info
  "Returns a map containing information about a container image. Info is looked up by the image UUID."
  [ImageID]
  (first (select container-images
                 (fields :name :tag :url :id)
                 (where {:id (uuidify ImageID)}))))

(defn tool-image-info
  "Returns a map containing information about a container image. Info is looked up by the tool UUID"
  [ToolIdParam]
  (let [image-id (:container_images_id
                  (first (select tools
                                 (fields :container_images_id)
                                 (where {:id (uuidify ToolIdParam)}))))]
    (image-info image-id)))

(defn- get-tag
  [ImageSpecifier]
  (if-not (contains? ImageSpecifier :tag)
    "latest"
    (:tag ImageSpecifier)))

(defn image?
  "Returns true if the given name and tag exist in the container_images table."
  [ImageSpecifier]
  (let [tag  (get-tag ImageSpecifier)
        name (:name ImageSpecifier)]
    (pos?
     (count
      (select container-images
              (where (and (= :name name)
                          (= :tag tag))))))))

(defn image-id
  "Returns the UUID used as the primary key in the container_images table."
  [ImageSpecifier]
  (let [tag  (get-tag ImageSpecifier)
        name (:name ImageSpecifier)]
    (if-not (image? ImageSpecifier)
      (throw (Exception. (str "image does not exist: " ImageSpecifier)))
      (:id (first (select container-images
                          (where (and (= :name name)
                                      (= :tag tag)))))))))

(defn add-image-info
  [ImageSpecifier]
  (let [tag  (get-tag ImageSpecifier)
        name (:name ImageSpecifier)
        url  (:url ImageSpecifier)]
    (when-not (image? ImageSpecifier)
      (insert container-images
              (values {:name name
                       :tag tag
                       :url url})))))

(defn modify-image-info
  "Updates the record for a container image. Basically, just allows you to set a new URL
   at this point."
  [ImageSpecifier]
  (let [tag  (get-tag ImageSpecifier)
        name (:name ImageSpecifier)
        url  (:url ImageSpecifier)]
    (if-not (image? ImageSpecifier)
      (throw (Exception. (str "image doesn't exist: " ImageSpecifier)))
      (update container-images
              (set-fields {:url url})
              (where (and (= :name name)
                          (= :tag tag)))))))

(defn delete-image-info
  "Deletes a record for an image"
  [ImageSpecifier]
  (when (image? ImageSpecifier)
    (let [tag  (get-tag ImageSpecifier)
          name (:name ImageSpecifier)]
      (transaction
       (update tools
               (set-fields {:container_images_id nil})
               (where {:container_images_id (image-id ImageSpecifier)}))
       (delete container-images
               (where (and (= :name name)
                           (= :tag tag))))))))

(defn devices
  "Returns the devices associated with the given container_setting uuid."
  [DeviceID]
  (select container-devices
          (where {:container_settings_id (uuidify DeviceID)})))

(defn device
  "Returns the device indicated by the UUID."
  [DeviceID]
  (first (select container-devices
                 (where {:id (uuidify DeviceID)}))))

(defn device?
  "Returns true if the given UUID is associated with a device."
  [DeviceID]
  (pos? (count (select container-devices (where {:id (uuidify DeviceID)})))))

(defn device-mapping?
  "Returns true if the combination of container_settings UUID, host-path, and
   container-path already exists in the container_devices table."
  [SettingsID DeviceHostPath DeviceContainerPath]
  (pos? (count (select container-devices (where (and (= :container_settings_id (uuidify SettingsID))
                                                     (= :host_path DeviceHostPath)
                                                     (= :container_path DeviceContainerPath)))))))

(defn settings-has-device?
  "Returns true if the container_settings record specified by the given UUID has
   at least one device associated with it."
  [SettingsID]
  (pos? (count (select container-devices (where {:container_settings_id (uuidify SettingsID)})))))

(defn add-device
  "Associates a device with the given container_settings UUID."
  [SettingsID DeviceHostPath DeviceContainerPath]
  (if (device-mapping? SettingsID DeviceHostPath DeviceContainerPath)
    (throw (Exception. (str "device mapping already exists: " SettingsID " " DeviceHostPath " " DeviceContainerPath))))
  (insert container-devices
          (values {:container_settings_id (uuidify SettingsID)
                   :host_path DeviceHostPath
                   :container_path DeviceContainerPath})))

(defn modify-device
  [DeviceID SettingsID DeviceHostPath DeviceContainerPath]
  (if-not (device? DeviceID)
    (throw (Exception. (str "device does not exist: " DeviceID))))
  (update container-devices
          (set-fields {:container_settings_id (uuidify SettingsID)
                       :host_path DeviceHostPath
                       :container_path DeviceContainerPath})
          (where {:id (uuidify DeviceID)})))

(defn delete-device
  [DeviceID]
  (if (device? DeviceID)
    (delete container-devices
            (where {:id (uuidify DeviceID)}))))

(defn volumes
  "Returns the devices associated with the given container_settings UUID."
  [SettingsID]
  (select container-volumes (where {:container_settings_id (uuidify SettingsID)})))

(defn volume
  "Returns the volume indicated by the UUID."
  [VolumeID]
  (first (select container-volumes (where {:id (uuidify VolumeID)}))))

(defn volume?
  "Returns true if volume indicated by the UUID exists."
  [VolumeID]
  (pos? (count (select container-volumes (where {:id (uuidify VolumeID)})))))

(defn volume-mapping?
  "Returns true if the combination of container_settings UUID, host-path, and
   container-path already exists in the database."
  [SettingsID VolumeHostPath VolumeContainerPath]
  (pos? (count (select container-volumes
                       (where (and (= :container_settings_id (uuidify SettingsID))
                                   (= :host_path VolumeHostPath)
                                   (= :container_path VolumeContainerPath)))))))
(defn settings-has-volume?
  "Returns true if the container_settings UUID has at least one volume
   associated with it."
  [SettingsID]
  (pos? (count (select container-volumes (where {:container_settings_id (uuidify SettingsID)})))))

(defn add-volume
  "Adds a volume record to the database for the specified container_settings UUID."
  [SettingsID VolumeHostPath VolumeContainerPath]
  (if (volume-mapping? SettingsID VolumeHostPath VolumeContainerPath)
    (throw (Exception. (str "volume mapping already exists: " SettingsID " " VolumeHostPath " " VolumeContainerPath))))
  (insert container-volumes
          (values {:container_settings_id (uuidify SettingsID)
                   :host_path VolumeHostPath
                   :container_path VolumeContainerPath})))

(defn modify-volume
  "Modifies the container_volumes record indicated by the uuid."
  [VolumeID SettingsID VolumeHostPath VolumeContainerPath]
  (if-not (volume? VolumeID)
    (throw (Exception. (str "volume does not exist: " VolumeID))))
  (update container-volumes
          (set-fields {:container_settings_id (uuidify SettingsID)
                       :host_path VolumeHostPath
                       :container_path VolumeContainerPath})
          (where {:id (uuidify VolumeID)})))

(defn delete-volume
  "Deletes the volume associated with uuid in the container_volumes table."
  [VolumeID]
  (when (volume? VolumeID)
    (delete container-volumes (where {:id (uuidify VolumeID)}))))

(defn volumes-from
  "Returns all of the records from the container_volumes_from table that are associated
   with the given container_settings UUID."
  [SettingsID]
  (select container-volumes-from (where {:container_settings_id (uuidify SettingsID)})))

(defn volume-from
  "Returns all records from container_volumes_from associated with the UUID passed in. There
   should only be a single result, but we're returning a seq just in case."
  [VolumesFromID]
  (first (select container-volumes-from
                 (where {:id (uuidify VolumesFromID)}))))

(defn volume-from?
  "Returns true if the volume_from record indicated by the UUID exists."
  [VolumesFromID]
  (pos? (count (select container-volumes-from
                       (where {:id (uuidify VolumesFromID)})))))

(defn volume-from-mapping?
  "Returns true if the combination of the container_settings UUID and container
   already exists in the container_volumes_from table."
  [SettingsID VolumesFromName]
  (pos? (count (select container-volumes-from
                       (where {:container_settings_id (uuidify SettingsID)
                               :name VolumesFromName})))))

(defn settings-has-volume-from?
  "Returns true if the indicated container_settings record has at least one
   container_volumes_from record associated with it."
  [SettingsID]
  (pos? (count (select container-volumes-from
                       (where {:container_settings_id (uuidify SettingsID)})))))

(defn add-volume-from
  "Adds a record to container_volumes_from associated with the given
   container_settings UUID."
  [SettingsID VolumesFromName]
  (if (settings-has-volume-from? SettingsID)
    (throw (Exception. (str "volume from mapping already exists: " SettingsID " " VolumesFromName))))
  (insert container-volumes-from
          (values {:container_settings_id (uuidify SettingsID)
                   :name VolumesFromName})))

(defn modify-volume-from
  "Modifies a record in container_volumes_from."
  [VolumesFromID SettingsID VolumesFromName]
  (if-not (volume-from? VolumesFromID)
    (throw (Exception. (str "volume from setting does not exist: " VolumesFromID))))
  (update container-volumes-from
          (set-fields {:container_settings_id (uuidify SettingsID)
                      :name VolumesFromName})
          (where {:id (uuidify VolumesFromID)})))

(defn delete-volume-from
  "Deletes a record from container_volumes_from."
  [VolumesFromID]
  (when (volume-from? VolumesFromID)
    (delete container-volumes-from
            (where {:id (uuidify VolumesFromID)}))))

(defn settings
  "Returns the settings associated with the given UUID."
  [SettingsID]
  (first (select container-settings
                 (where {:id (uuidify SettingsID)}))))

(defn settings?
  "Returns true if the given UUID is associated with a set of container settings."
  [SettingsID]
  (pos? (count (select container-settings (where {:id (uuidify SettingsID)})))))

(defn- filter-params
  [Settings]
  (into {} (filter
            (fn [[k v]]
              (contains?
               #{:cpu_shares
                 :memory_limit
                 :network_mode
                 :working_directory
                 :name
                 :tools_id}
               k))
            Settings)))

(defn add-settings
  "Adds a new settings record to the database based on the parameter map."
  [Settings]
  (insert container-settings
          (values (filter-params Settings))))

(defn tool-has-settings?
  "Returns true if the given tool UUID has some container settings associated with it."
  [ToolIdParam]
  (pos? (count (select container-settings (where {:tools_id (uuidify ToolIdParam)})))))

(defn modify-settings
  "Modifies an existing set of container settings. Requires the container-settings-uuid
   and a new set of values."
  [SettingsID Settings]
  (if-not (settings? SettingsID)
    (throw (Exception. (str "Container settings do not exist for UUID: " SettingsID))))
  (let [values (filter-params Settings)]
    (update container-settings
            (set-fields values)
            (where {:id (uuidify SettingsID)}))))

(defn delete-settings
  "Deletes an existing set of container settings. Requires the container-settings uuid."
  [SettingsID]
  (when (settings? SettingsID)
    (let [id (uuidify SettingsID)]
      (transaction
       (delete container-devices
               (where {:container_settings_id id}))
       (delete container-volumes
               (where {:container_settings_id id}))
       (delete container-volumes-from
               (where {:container_settings_id id}))
       (delete container-settings
               (where {:id id}))))))

(defn tool-container-info
  "Returns container info associated with a tool or nil"
  [ToolIdParam]
  (let [id (uuidify ToolIdParam)]
    (when (tool-has-settings? id)
      (->  (select container-settings
                   (fields :id :cpu_shares :memory_limit :network_mode :name :working_directory)
                   (with container-devices
                         (fields :host_path :container_path :id))
                   (with container-volumes
                         (fields :host_path :container_path :id))
                   (with container-volumes-from
                         (fields :name :id))
                   (where {:tools_id id}))
           first
           (merge {:image (tool-image-info ToolIdParam)})))))

(defn all-settings
  "Returns a map with all of the settings for a container, including all of the
   devices, volumes, and volumes-froms."
  [SettingsID]
  (let [id    (uuidify SettingsID)
        rm-id (fn [m] (dissoc m :container_settings_id))]
    (-> (settings id)
        (assoc :devices (mapv rm-id (devices id)))
        (assoc :volumes (mapv rm-id (volumes id)))
        (assoc :volumes-from (mapv rm-id (volumes-from id))))))

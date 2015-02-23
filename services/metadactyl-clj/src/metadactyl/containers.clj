(ns metadactyl.containers
  (:use [kameleon.core]
        [kameleon.entities :only [tools
                                  container-images
                                  container-settings
                                  container-devices
                                  container-volumes
                                  container-volumes-from]]
        [kameleon.uuids :only [uuidify]]
        [korma.core]
        [korma.db :only [transaction]])
  (:require [clojure.tools.logging :as log]))

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
  [image-uuid]
  (first (select container-images
                 (fields :name :tag :url :id)
                 (where {:id (uuidify image-uuid)}))))

(defn tool-image-info
  "Returns a map containing information about a container image. Info is looked up by the tool UUID"
  [tool-uuid]
  (let [image-id (:container_images_id
                  (first (select tools
                                 (fields :container_images_id)
                                 (where {:id (uuidify tool-uuid)}))))]
    (image-info image-id)))

(defn- get-tag
  [image-map]
  (if-not (contains? image-map :tag)
    "latest"
    (:tag image-map)))

(defn image?
  "Returns true if the given name and tag exist in the container_images table."
  [image-map]
  (let [tag  (get-tag image-map)
        name (:name image-map)]
    (pos?
     (count
      (select container-images
              (where (and (= :name name)
                          (= :tag tag))))))))

(defn image-id
  "Returns the UUID used as the primary key in the container_images table."
  [image-map]
  (let [tag  (get-tag image-map)
        name (:name image-map)]
    (if-not (image? image-map)
      (throw (Exception. (str "image does not exist: " image-map)))
      (:id (first (select container-images
                          (where (and (= :name name)
                                      (= :tag tag)))))))))

(defn add-image-info
  [image-map]
  (let [tag  (get-tag image-map)
        name (:name image-map)
        url  (:url image-map)]
    (when-not (image? image-map)
      (insert container-images
              (values {:name name
                       :tag tag
                       :url url})))))

(defn modify-image-info
  "Updates the record for a container image. Basically, just allows you to set a new URL
   at this point."
  [image-map]
  (let [tag  (get-tag image-map)
        name (:name image-map)
        url  (:url image-map)]
    (if-not (image? image-map)
      (throw (Exception. (str "image doesn't exist: " image-map)))
      (update container-images
              (set-fields {:url url})
              (where (and (= :name name)
                          (= :tag tag)))))))

(defn delete-image-info
  "Deletes a record for an image"
  [image-map]
  (when (image? image-map)
    (let [tag  (get-tag image-map)
          name (:name image-map)]
      (transaction
       (update tools
               (set-fields {:container_images_id nil})
               (where {:container_images_id (image-id image-map)}))
       (delete container-images
               (where (and (= :name name)
                           (= :tag tag))))))))

(defn devices
  "Returns the devices associated with the given container_setting uuid."
  [settings-uuid]
  (select container-devices
          (where {:container_settings_id (uuidify settings-uuid)})))

(defn device
  "Returns the device indicated by the UUID."
  [device-uuid]
  (first (select container-devices
                 (where {:id (uuidify device-uuid)}))))

(defn device?
  "Returns true if the given UUID is associated with a device."
  [device-uuid]
  (pos? (count (select container-devices (where {:id (uuidify device-uuid)})))))

(defn device-mapping?
  "Returns true if the combination of container_settings UUID, host-path, and
   container-path already exists in the container_devices table."
  [settings-uuid host-path container-path]
  (pos? (count (select container-devices (where (and (= :container_settings_id (uuidify settings-uuid))
                                                     (= :host_path host-path)
                                                     (= :container_path container-path)))))))

(defn settings-has-device?
  "Returns true if the container_settings record specified by the given UUID has
   at least one device associated with it."
  [settings-uuid device-uuid]
  (pos? (count (select container-devices
                       (where {:container_settings_id (uuidify settings-uuid)
                               :id                    (uuidify device-uuid)})))))

(defn add-device
  "Associates a device with the given container_settings UUID."
  [settings-uuid host-path container-path]
  (if (device-mapping? settings-uuid host-path container-path)
    (throw (Exception. (str "device mapping already exists: " settings-uuid " " host-path " " container-path))))
  (insert container-devices
          (values {:container_settings_id (uuidify settings-uuid)
                   :host_path host-path
                   :container_path container-path})))

(defn modify-device
  [device-uuid settings-uuid host-path container-path]
  (if-not (device? device-uuid)
    (throw (Exception. (str "device does not exist: " device-uuid))))
  (update container-devices
          (set-fields {:container_settings_id (uuidify settings-uuid)
                       :host_path host-path
                       :container_path container-path})
          (where {:id (uuidify device-uuid)})))

(defn delete-device
  [device-uuid]
  (if (device? device-uuid)
    (delete container-devices
            (where {:id (uuidify device-uuid)}))))

(defn volumes
  "Returns the devices associated with the given container_settings UUID."
  [settings-uuid]
  (select container-volumes (where {:container_settings_id (uuidify settings-uuid)})))

(defn volume
  "Returns the volume indicated by the UUID."
  [volume-uuid]
  (first (select container-volumes (where {:id (uuidify volume-uuid)}))))

(defn volume?
  "Returns true if volume indicated by the UUID exists."
  [volume-uuid]
  (pos? (count (select container-volumes (where {:id (uuidify volume-uuid)})))))

(defn volume-mapping?
  "Returns true if the combination of container_settings UUID, host-path, and
   container-path already exists in the database."
  [settings-uuid host-path container-path]
  (pos? (count (select container-volumes
                       (where (and (= :container_settings_id (uuidify settings-uuid))
                                   (= :host_path host-path)
                                   (= :container_path container-path)))))))
(defn settings-has-volume?
  "Returns true if the container_settings UUID has at least one volume
   associated with it."
  [settings-uuid volume-uuid]
  (pos? (count (select container-volumes
                       (where {:container_settings_id (uuidify settings-uuid)
                               :id                    (uuidify volume-uuid)})))))

(defn add-volume
  "Adds a volume record to the database for the specified container_settings UUID."
  [settings-uuid host-path container-path]
  (if (volume-mapping? settings-uuid host-path container-path)
    (throw (Exception. (str "volume mapping already exists: " settings-uuid " " host-path " " container-path))))
  (insert container-volumes
          (values {:container_settings_id (uuidify settings-uuid)
                   :host_path host-path
                   :container_path container-path})))

(defn modify-volume
  "Modifies the container_volumes record indicated by the uuid."
  [volume-uuid settings-uuid host-path container-path]
  (if-not (volume? volume-uuid)
    (throw (Exception. (str "volume does not exist: " volume-uuid))))
  (update container-volumes
          (set-fields {:container_settings_id (uuidify settings-uuid)
                       :host_path host-path
                       :container_path container-path})
          (where {:id (uuidify volume-uuid)})))

(defn delete-volume
  "Deletes the volume associated with uuid in the container_volumes table."
  [volume-uuid]
  (when (volume? volume-uuid)
    (delete container-volumes (where {:id (uuidify volume-uuid)}))))

(defn volumes-from
  "Returns all of the records from the container_volumes_from table that are associated
   with the given container_settings UUID."
  [settings-uuid]
  (select container-volumes-from (where {:container_settings_id (uuidify settings-uuid)})))

(defn volume-from
  "Returns all records from container_volumes_from associated with the UUID passed in. There
   should only be a single result, but we're returning a seq just in case."
  [volumes-from-uuid]
  (first (select container-volumes-from
                 (where {:id (uuidify volumes-from-uuid)}))))

(defn volume-from?
  "Returns true if the volume_from record indicated by the UUID exists."
  [volumes-from-uuid]
  (pos? (count (select container-volumes-from
                       (where {:id (uuidify volumes-from-uuid)})))))

(defn volume-from-mapping?
  "Returns true if the combination of the container_settings UUID and container
   already exists in the container_volumes_from table."
  [settings-uuid volumes-from-name]
  (pos? (count (select container-volumes-from
                       (where {:container_settings_id (uuidify settings-uuid)
                               :name volumes-from-name})))))

(defn settings-has-volumes-from?
  "Returns true if the indicated container_settings record has at least one
   container_volumes_from record associated with it."
  [settings-uuid volumes-from-uuid]
  (pos? (count (select container-volumes-from
                       (where {:container_settings_id (uuidify settings-uuid)
                               :id                    (uuidify volumes-from-uuid)})))))

(defn add-volume-from
  "Adds a record to container_volumes_from associated with the given
   container_settings UUID."
  [settings-uuid volumes-from-name]
  (insert container-volumes-from
          (values {:container_settings_id (uuidify settings-uuid)
                   :name volumes-from-name})))

(defn modify-volume-from
  "Modifies a record in container_volumes_from."
  [volumes-from-uuid settings-uuid volumes-from-name]
  (if-not (volume-from? volumes-from-uuid)
    (throw (Exception. (str "volume from setting does not exist: " volumes-from-uuid))))
  (update container-volumes-from
          (set-fields {:container_settings_id (uuidify settings-uuid)
                      :name volumes-from-name})
          (where {:id (uuidify volumes-from-uuid)})))

(defn delete-volume-from
  "Deletes a record from container_volumes_from."
  [volumes-from-uuid]
  (when (volume-from? volumes-from-uuid)
    (delete container-volumes-from
            (where {:id (uuidify volumes-from-uuid)}))))

(defn settings
  "Returns the settings associated with the given UUID."
  [settings-uuid]
  (first (select container-settings
                 (where {:id (uuidify settings-uuid)}))))

(defn settings?
  "Returns true if the given UUID is associated with a set of container settings."
  [settings-uuid]
  (pos? (count (select container-settings (where {:id (uuidify settings-uuid)})))))

(defn- filter-params
  [settings-map]
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
            settings-map)))

(defn add-settings
  "Adds a new settings record to the database based on the parameter map."
  [settings-map]
  (insert container-settings
          (values (filter-params settings-map))))

(defn tool-has-settings?
  "Returns true if the given tool UUID has some container settings associated with it."
  [tool-uuid]
  (pos? (count (select container-settings (where {:tools_id (uuidify tool-uuid)})))))

(defn tool-settings-uuid
  "Returns the container_settings UUID for the given tool UUID."
  [tool-uuid]
  (:id (first (select container-settings (where {:tools_id (uuidify tool-uuid)})))))

(defn modify-settings
  "Modifies an existing set of container settings. Requires the container-settings-uuid
   and a new set of values."
  [settings-uuid settings-map]
  (if-not (settings? settings-uuid)
    (throw (Exception. (str "Container settings do not exist for UUID: " settings-uuid))))
  (let [values (filter-params settings-map)]
    (update container-settings
            (set-fields values)
            (where {:id (uuidify settings-uuid)}))))

(defn delete-settings
  "Deletes an existing set of container settings. Requires the container-settings uuid."
  [settings-uuid]
  (when (settings? settings-uuid)
    (let [id (uuidify settings-uuid)]
      (transaction
       (delete container-devices
               (where {:container_settings_id id}))
       (delete container-volumes
               (where {:container_settings_id id}))
       (delete container-volumes-from
               (where {:container_settings_id id}))
       (delete container-settings
               (where {:id id}))))))

(defn tool-settings
  "Returns the top-level settings for the tool container."
  [tool-uuid]
  (first (select container-settings (where {:tools_id (uuidify tool-uuid)}))))

(defn tool-container-info
  "Returns container info associated with a tool or nil"
  [tool-uuid]
  (let [id (uuidify tool-uuid)]
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
           (merge {:image (tool-image-info tool-uuid)})))))

(defn tool-cpu-shares
  "Returns the cpu shares allocated to the tool container."
  [tool-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings (tool-settings tool-uuid)]
      {:cpu_shares (:cpu_shares settings)})))

(defn update-settings-field
  [tool-uuid field-kw new-value]
  (let [id (uuidify tool-uuid)]
    (when (tool-has-settings? id)
      (let [settings-id (tool-settings-uuid id)]
        (select-keys (modify-settings settings-id {field-kw new-value}) [field-kw])))))

(defn tool-memory-limit
  "Returns the maximum amount of RAM (in bytes) that will be allocated to the tool container."
  [tool-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings (tool-settings tool-uuid)]
      {:memory_limit (:memory_limit settings)})))

(defn tool-network-mode
  "Returns the network mode that the tool container will use."
  [tool-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings (tool-settings tool-uuid)]
      {:network_mode (:network_mode settings)})))

(defn tool-working-directory
  "Returns the working directory for the tool container."
  [tool-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings (tool-settings tool-uuid)]
      {:working_directory (:working_directory settings)})))

(defn tool-container-name
  "Returns the name of the tool container."
  [tool-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings (tool-settings tool-uuid)]
      {:name (:name settings)})))

(defn tool-device-info
  "Returns a container's device information based on the tool UUID."
  [tool-uuid]
  (let [container-info (tool-container-info tool-uuid)]
    (if-not (nil? container-info)
      {:container_devices (:container_devices container-info)})))

(defn tool-device
  "Returns a map with information about a particular device associated with the tools container."
  [tool-uuid device-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings-uuid (tool-settings-uuid tool-uuid)]
      (when (settings-has-device? settings-uuid device-uuid)
        (dissoc (device device-uuid) :container_settings_id)))))

(defn tool-volume
  "Returns a map with info about a particular volume associated with the tool's container."
  [tool-uuid volume-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings-uuid (tool-settings-uuid tool-uuid)]
      (when (settings-has-volume? settings-uuid volume-uuid)
        (dissoc (volume volume-uuid) :container_settings_id)))))

(defn tool-volumes-from
  "Returns a map with info about a particular container from which the tool's container will mount volumes."
  [tool-uuid volumes-from-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings-uuid (tool-settings-uuid tool-uuid)]
      (when (settings-has-volumes-from? settings-uuid volumes-from-uuid)
        (dissoc (volume-from volumes-from-uuid) :container_settings_id)))))

(defn tool-volume-info
  "Returns a container's volumes info based on the tool UUID."
  [tool-uuid]
  (let [container-info (tool-container-info tool-uuid)]
    (if-not (nil? container-info)
      {:container_volumes (:container_volumes container-info)})))

(defn tool-volumes-from-info
  "Returns a container's volumes-from info based on the tool UUID."
  [tool-uuid]
  (let [container-info (tool-container-info tool-uuid)]
    (if-not (nil? container-info)
      {:container_volumes_from (:container_volumes_from container-info)})))

(defn all-settings
  "Returns a map with all of the settings for a container, including all of the
   devices, volumes, and volumes-froms."
  [settings-uuid]
  (let [id    (uuidify settings-uuid)
        rm-id (fn [m] (dissoc m :container_settings_id))]
    (-> (settings id)
        (assoc :devices (mapv rm-id (devices id)))
        (assoc :volumes (mapv rm-id (volumes id)))
        (assoc :volumes-from (mapv rm-id (volumes-from id))))))

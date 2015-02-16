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
        [korma.db :only [transaction]]))

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
  [image-id]
  (first (select container-images
                 (where {:id (uuidify image-id)}))))

(defn image?
  "Returns true if the given name and tag exist in the container_images table."
  [{name :name tag :tag :or {tag "latest"}}]
  (pos?
   (count
    (select container-images
            (where (and (= :name name)
                        (= :tag tag)))))))

(defn image-id
  "Returns the UUID used as the primary key in the container_images table."
  [{name :name tag :tag :or {tag "latest"} :as params}]
  (if-not (image? params)
    (throw (Exception. (str "image does not exist: " params)))
    (:id (first (select container-images
                        (where (and (= :name name)
                                    (= :tag tag))))))))

(defn add-image-info
  "Inserts info about a new container image into the database. The parameter map
   requires the name field, but the url field is optional. Tag defaults to latest"
  [{name :name tag :tag url :url :or {tag "latest"} :as params}]
  (when-not (image? params)
    (insert container-images
            (values {:name name
                     :tag tag
                     :url url}))))

(defn modify-image-info
  "Updates the record for a container image. Basically, just allows you to set a new URL
   at this point."
  [{name :name tag :tag url :url :or {tag "latest"} :as params}]
  (if-not (image? params)
    (throw (Exception. (str "image doesn't exist: " params)))
    (update container-images
            (set-fields {:url url})
            (where (and (= :name name)
                        (= :tag tag))))))

(defn delete-image-info
  "Deletes a record for an image"
  [{name :name tag :tag :as params}]
  (when (image? params)
    (transaction
     (update tools
             (set-fields {:container_images_id nil})
             (where {:container_images_id (image-id params)}))
     (delete container-images
             (where (and (= :name name)
                         (= :tag tag)))))))

(defn settings
  "Returns the settings associated with the given UUID."
  [uuid]
  (first (select container-settings
                 (where {:id (uuidify uuid)}))))

(defn settings?
  "Returns true if the given UUID is associated with a set of container settings."
  [uuid]
  (pos? (count (select container-settings (where {:id (uuidify uuid)})))))

(defn- filter-params
  [params]
  (into {} (filter
            (fn [[k v]]
              (contains?
               #{:cpu_shares
                 :memory_limit
                 :network_mode
                 :working_directory
                 :name}
               k))
            params)))

(defn add-settings
  "Adds a new settings record to the database based on the parameter map.
   None of the fields are required. Recognized fields are:
     :cpu_shares - integer granting shares of the CPU to the container.
     :memory_limit - bigint number of bytes of RAM to give to the container.
     :network_mode - either bridge or none
     :working_directory - default working directory for the container
     :name - name to give the container
   Does not check to see if the record already exists, since multiple containers
   have the same settings. Trying to dedupe would just make editing settings
   more complicated."
  [params]
  (insert container-settings
          (values (filter-params params))))

(defn modify-settings
  "Modifies an existing set of container settings. Requires the container-settings-uuid
   and a new set of values."
  [uuid params]
  (if-not (settings? uuid)
    (throw (Exception. (str "Container settings do not exist for UUID: " uuid))))
  (let [values (filter-params params)]
    (update container-settings
            (set-fields values)
            (where {:id (uuidify uuid)}))))

(defn delete-settings
  "Deletes an existing set of container settings. Requires the container-settings uuid."
  [uuid]
  (when (settings? uuid)
    (delete container-settings
            (where {:id (uuidify uuid)}))))

(defn devices
  "Returns the devices associated with the given container_setting uuid."
  [settings-uuid]
  (select container-devices
          (where {:container_settings_id (uuidify settings-uuid)})))

(defn device
  "Returns the device indicated by the UUID."
  [uuid]
  (first (select container-devices
                 (where {:id (uuidify uuid)}))))

(defn device?
  "Returns true if the given UUID is associated with a device."
  [uuid]
  (pos? (count (select container-devices (where {:id (uuidify uuid)})))))

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
  [settings-uuid]
  (pos? (count (select container-devices (where {:container_settings_id (uuidify settings-uuid)})))))

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
  [uuid settings-uuid host-path container-path]
  (if-not (device? uuid)
    (throw (Exception. (str "device does not exist: " uuid))))
  (update container-devices
          (set-fields {:container_settings_id (uuidify settings-uuid)
                       :host_path host-path
                       :container_path container-path})
          (where {:id (uuidify uuid)})))

(defn delete-device
  [uuid]
  (if (device? uuid)
    (delete container-devices
            (where {:id (uuidify uuid)}))))

(defn volumes
  "Returns the devices associated with the given container_settings UUID."
  [settings-uuid]
  (select container-volumes (where {:container_settings_id (uuidify settings-uuid)})))

(defn volume
  "Returns the volume indicated by the UUID."
  [uuid]
  (first (select container-volumes (where {:id (uuidify uuid)}))))

(defn volume?
  "Returns true if volume indicated by the UUID exists."
  [uuid]
  (pos? (count (select container-volumes (where {:id (uuidify uuid)})))))

(defn volume-mapping?
  "Returns true if the combination of container_settings UUID, host-path, and
   container-path already exists in the database."
  [settings-uuid host-path container-path]
  (pos? (count (select container-volumes (where (and (= :container_settings_id (uuidify settings-uuid))
                                                     (= :host_path host-path)
                                                     (= :container_path container-path)))))))
(defn settings-has-volume?
  "Returns true if the container_settings UUID has at least one volume
   associated with it."
  [settings-uuid]
  (pos? (count (select container-volumes (where {:container_settings_id (uuidify settings-uuid)})))))

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
  [uuid settings-uuid host-path container-path]
  (if-not (volume? uuid)
    (throw (Exception. (str "volume does not exist: " uuid))))
  (update container-volumes
          (set-fields {:container_settings_id (uuidify settings-uuid)
                       :host_path host-path
                       :container_path container-path})
          (where {:id (uuidify uuid)})))

(defn delete-volume
  "Deletes the volume associated with uuid in the container_volumes table."
  [uuid]
  (when (volume? uuid)
    (delete container-volumes (where {:id (uuidify uuid)}))))

(defn volumes-from
  "Returns all of the records from the container_volumes_from table that are associated
   with the given container_settings UUID."
  [settings-uuid]
  (select container-volumes-from (where {:container_settings_id (uuidify settings-uuid)})))

(defn volume-from
  "Returns all records from container_volumes_from associated with the UUID passed in. There
   should only be a single result, but we're returning a seq just in case."
  [uuid]
  (first (select container-volumes-from
                 (where {:id (uuidify uuid)}))))

(defn volume-from?
  "Returns true if the volume_from record indicated by the UUID exists."
  [uuid]
  (pos? (count (select container-volumes-from
                       (where {:id (uuidify uuid)})))))

(defn volume-from-mapping?
  "Returns true if the combination of the container_settings UUID and container
   already exists in the container_volumes_from table."
  [settings-uuid name]
  (pos? (count (select container-volumes-from
                       (where {:container_settings_id (uuidify settings-uuid)
                               :name name})))))

(defn settings-has-volume-from?
  "Returns true if the indicated container_settings record has at least one
   container_volumes_from record associated with it."
  [settings-uuid]
  (pos? (count (select container-volumes-from
                       (where {:container_settings_id (uuidify settings-uuid)})))))

(defn add-volume-from
  "Adds a record to container_volumes_from associated with the given
   container_settings UUID."
  [settings-uuid container-name]
  (if (settings-has-volume-from? settings-uuid)
    (throw (Exception. (str "volume from mapping already exists: " settings-uuid " " container-name))))
  (insert container-volumes-from
          (values {:container_settings_id (uuidify settings-uuid)
                   :name container-name})))

(defn modify-volume-from
  "Modifies a record in container_volumes_from."
  [uuid settings-uuid container-name]
  (if-not (volume-from? uuid)
    (throw (Exception. (str "volume from setting does not exist: " uuid))))
  (update container-volumes-from
          (set-fields {:container_settings_id (uuidify settings-uuid)
                      :name container-name})
          (where {:id (uuidify uuid)})))

(defn delete-volume-from
  "Deletes a record from container_volumes_from."
  [uuid]
  (when (volume-from? uuid)
    (delete container-volumes-from
            (where {:id (uuidify uuid)}))))

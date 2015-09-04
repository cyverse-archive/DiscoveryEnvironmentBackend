(ns metadactyl.containers
  (:use [kameleon.core]
        [kameleon.entities :only [tools
                                  container-images
                                  container-settings
                                  container-devices
                                  container-volumes
                                  container-volumes-from
                                  data-containers]]
        [kameleon.uuids :only [uuidify]]
        [korma.core]
        [korma.db :only [transaction]]
        [metadactyl.persistence.app-metadata :only [update-tool]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.conversions :only [remove-nil-vals remove-empty-vals]])
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

(defn list-images
  "Returns a list of all defined images."
  []
  {:container_images
    (select container-images
      (fields :name :tag :url :id))})

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
  (let [image-values (-> image-map
                         (select-keys [:name])
                         (assoc :tag (get-tag image-map)))]
    (:id (first (select container-images (where image-values))))))

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

(defn- find-or-add-image-id
  [image]
  (or (image-id image)
      (:id (add-image-info image))))

(defn modify-image-info
  "Updates the record for a container image. Basically, just allows you to set a new URL
   at this point."
  [image-id update-map]
  (let [umap (select-keys update-map [:name :tag :url])]
    (when-not (empty? umap)
      (update container-images
        (set-fields umap)
        (where (= :id (uuidify image-id))))
      (select container-images
        (where (= :id (uuidify image-id)))))))

(defn delete-image
  [image-id]
  (when (image-id image-id)
    (transaction
      (update tools
        (set-fields {:container_images_id nil})
        (where {:container_images_id (uuidify image-id)}))
      (delete container-images
        (where (= :id (uuidify image-id)))))))

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

(defn device-mapping
  [settings-uuid host-path container-path]
  (first (select container-devices (where (and (= :host_path host-path)
                                               (= :container_path container-path)
                                               (= :container_settings_id (uuidify settings-uuid)))))))

(defn settings-has-device?
  "Returns true if the container_settings record specified by the given UUID has
   at least one device associated with it."
  [settings-uuid device-uuid]
  (pos? (count (select container-devices
                       (where {:container_settings_id (uuidify settings-uuid)
                               :id                    (uuidify device-uuid)})))))

(defn add-device
  "Associates a device with the given container_settings UUID."
  [settings-uuid device-map]
  (if (device-mapping? settings-uuid (:host_path device-map) (:container_path device-map))
    (throw (Exception. (str "device mapping already exists: " settings-uuid " " (:host_path device-map)  " " (:container_path device-map)))))
  (insert container-devices
          (values (merge
                   (select-keys device-map [:host_path :container_path])
                   {:container_settings_id (uuidify settings-uuid)}))))

(defn modify-device
  [settings-uuid device-uuid update-map]
  (if-not (device? device-uuid)
    (throw (Exception. (str "device does not exist: " device-uuid))))
  (update container-devices
          (set-fields (select-keys update-map [:host_path :container_path :container_settings_id]))
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

(defn volume-mapping
  [settings-uuid host-path container-path]
  (first (select container-volumes
                 (where (and (= :container_settings_id (uuidify settings-uuid))
                             (= :host_path host-path)
                             (= :container_path container-path))))))

(defn settings-has-volume?
  "Returns true if the container_settings UUID has at least one volume
   associated with it."
  [settings-uuid volume-uuid]
  (pos? (count (select container-volumes
                       (where {:container_settings_id (uuidify settings-uuid)
                               :id                    (uuidify volume-uuid)})))))

(defn add-volume
  "Adds a volume record to the database for the specified container_settings UUID."
  [settings-uuid volume-map]
  (if (volume-mapping? settings-uuid (:host_path volume-map) (:container_path volume-map))
    (throw (Exception. (str "volume mapping already exists: " settings-uuid " " (:host_path volume-map) " " (:container_path volume-map)))))
  (insert container-volumes
          (values (merge
                   (select-keys volume-map [:host_path :container_path])
                   {:container_settings_id (uuidify settings-uuid)}))))

(defn modify-volume
  "Modifies the container_volumes record indicated by the uuid."
  [settings-uuid volume-uuid volume-map]
  (if-not (volume? volume-uuid)
    (throw (Exception. (str "volume does not exist: " volume-uuid))))
  (update container-volumes
          (set-fields (merge {:container_settings_id (uuidify settings-uuid)}
                             (select-keys volume-map [:host_path :container_path])))
          (where {:id (uuidify volume-uuid)})))

(defn delete-volume
  "Deletes the volume associated with uuid in the container_volumes table."
  [volume-uuid]
  (when (volume? volume-uuid)
    (delete container-volumes (where {:id (uuidify volume-uuid)}))))

(defn data-container
  "Returns a map describing a data container."
  [data-container-id]
  (assert-not-nil [:data-container-id data-container-id]
    (first
      (select data-containers
              (fields :id :name_prefix :read_only)
              (with container-images (fields :name :tag :url))
              (where {:id data-container-id})))))

(defn list-data-containers
  "Returns a list of all data containers."
  []
  {:data_containers (select data-containers
                      (fields :id :name_prefix :read_only)
                      (with container-images (fields :name :tag :url)))})

(defn- add-data-container
  [data-container-info]
  (let [image-uuid (find-or-add-image-id data-container-info)
        insert-values (assoc (select-keys data-container-info [:name_prefix :read_only])
                             :container_images_id image-uuid)]
    (insert data-containers (values insert-values))))

(defn modify-data-container
  "Modifies a data container based on the update map."
  [data-container-id {:keys [name url] :as data-container-info}]
  (when (data-container data-container-id)
    (transaction
      (let [container-images-id (when name (find-or-add-image-id data-container-info))
            umap (-> data-container-info
                     (select-keys [:name_prefix :read_only])
                     (assoc :container_images_id container-images-id)
                     remove-nil-vals)]
        (when-not (empty? umap)
          (update data-containers
                  (set-fields umap)
                  (where {:id data-container-id})))
        (data-container data-container-id)))))

(defn- find-data-container-id
  "Returns the UUID used as the primary key in the data_containers table."
  [data-container-info]
  (:id (first
         (select data-containers
           (where (assoc (select-keys data-container-info [:name_prefix :read_only])
                         :container_images_id (image-id data-container-info)))))))

(defn volumes-from
  "Returns all records from container_volumes_from associated with the UUID passed in. There
   should only be a single result, but we're returning a seq just in case."
  [volumes-from-uuid]
  (first (select container-volumes-from
                 (where {:id (uuidify volumes-from-uuid)}))))

(defn volumes-from?
  "Returns true if the volume_from record indicated by the UUID exists."
  [volumes-from-uuid]
  (pos? (count (select container-volumes-from
                       (where {:id (uuidify volumes-from-uuid)})))))

(defn- volumes-from-mapping?
  "Returns true if the combination of the container_settings UUID and container
   already exists in the container_volumes_from table."
  [settings-uuid data-container-uuid]
  (pos? (count (select container-volumes-from
                       (where {:container_settings_id (uuidify settings-uuid)
                               :data_containers_id     (uuidify data-container-uuid)})))))

(defn- volumes-from-settings
  [settings-uuid data-container-uuid]
  (first (select container-volumes-from
                 (fields :id)
                 (with data-containers (fields :name_prefix :read_only)
                   (with container-images (fields :name :tag :url)))
                 (where {:container_settings_id (uuidify settings-uuid)
                         :data_containers_id     (uuidify data-container-uuid)}))))

(defn settings-has-volumes-from?
  "Returns true if the indicated container_settings record has at least one
   container_volumes_from record associated with it."
  [settings-uuid volumes-from-uuid]
  (pos? (count (select container-volumes-from
                       (where {:container_settings_id (uuidify settings-uuid)
                               :id                    (uuidify volumes-from-uuid)})))))

(defn- add-volumes-from
  "Adds a record to container_volumes_from associated with the given
   container_settings UUID."
  [settings-uuid data-container-uuid]
  (insert container-volumes-from
          (values {:container_settings_id (uuidify settings-uuid)
                   :data_containers_id     (uuidify data-container-uuid)})))

(defn delete-volumes-from
  "Deletes a record from container_volumes_from."
  [volumes-from-uuid]
  (when (volumes-from? volumes-from-uuid)
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

(defn- filter-container-settings
  [settings-map]
  (select-keys settings-map
    [:cpu_shares
     :memory_limit
     :network_mode
     :working_directory
     :name
     :entrypoint
     :tools_id]))

(defn add-settings
  "Adds a new settings record to the database based on the parameter map."
  [settings-map]
  (insert container-settings
          (values (filter-container-settings settings-map))))

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
  (let [values (filter-container-settings settings-map)]
    (update container-settings
            (set-fields values)
            (where {:id (uuidify settings-uuid)}))))

(defn tool-settings
  "Returns the top-level settings for the tool container."
  [tool-uuid]
  (first (select container-settings (where {:tools_id (uuidify tool-uuid)}))))

(defn filter-returns
  [retval]
  (-> retval remove-nil-vals remove-empty-vals))

(defn tool-container-info
  "Returns container info associated with a tool or nil. This is used to build
  the JSON map that is passed down to the JEX. If you make changes to the
  container-related parts of the DE database schema, you'll likely need to make
  changes here."
  [tool-uuid]
  (let [id (uuidify tool-uuid)]
    (when (tool-has-settings? id)
      (->  (select container-settings
                   (fields :id :cpu_shares :memory_limit :network_mode :name :working_directory :entrypoint)
                   (with container-devices
                     (fields :host_path :container_path :id))
                   (with container-volumes
                     (fields :host_path :container_path :id))
                   (with container-volumes-from
                     (fields :id)
                     (with data-containers
                       (fields :name_prefix :read_only)
                       (with container-images
                         (fields :name :tag :url))))
                   (where {:tools_id id}))
           first
           (merge {:image (tool-image-info tool-uuid)})
           filter-returns))))

(defn update-settings-field
  [tool-uuid field-kw new-value]
  (let [id (uuidify tool-uuid)]
    (when (tool-has-settings? id)
      (let [settings-id (tool-settings-uuid id)]
        (select-keys (modify-settings settings-id {field-kw new-value}) [field-kw])))))

(defn update-device-field
  [tool-uuid device-uuid field-kw new-value]
  (let [id (uuidify tool-uuid)]
    (when (tool-has-settings? id)
      (let [settings-id (tool-settings-uuid id)]
        (when (and (device? device-uuid)
                   (settings-has-device? settings-id device-uuid))
          (select-keys (modify-device settings-id device-uuid {field-kw new-value}) [field-kw]))))))

(defn get-settings-field
  [tool-uuid field-kw]
  (when (tool-has-settings? tool-uuid)
    (let [settings (tool-settings tool-uuid)]
      {field-kw (field-kw settings)})))

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

(defn add-tool-device
  [tool-uuid device-map]
  (when-not (tool-has-settings? tool-uuid)
    (throw (Exception. (str "Tool " tool-uuid " does not have a container."))))
  (let [settings-uuid (tool-settings-uuid tool-uuid)]
    (dissoc
     (if-not (device-mapping? settings-uuid (:host_path device-map) (:container_path device-map))
       (add-device settings-uuid device-map)
       (device-mapping settings-uuid (:host_path device-map) (:container_path device-map)))
     :container_settings_id)))

(defn device-field
  [tool-uuid device-uuid field-kw]
  (let [fields (tool-device tool-uuid device-uuid)]
    (or (select-keys fields [field-kw]) nil)))

(defn update-volume-field
  [tool-uuid volume-uuid field-kw new-value]
  (let [id (uuidify tool-uuid)]
    (when (tool-has-settings? id)
      (let [settings-id (tool-settings-uuid id)]
        (when (and (volume? volume-uuid)
                   (settings-has-volume? settings-id volume-uuid))
          (select-keys (modify-volume settings-id volume-uuid {field-kw new-value}) [field-kw]))))))

(defn tool-volume
  "Returns a map with info about a particular volume associated with the tool's container."
  [tool-uuid volume-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings-uuid (tool-settings-uuid tool-uuid)]
      (when (settings-has-volume? settings-uuid volume-uuid)
        (dissoc (volume volume-uuid) :container_settings_id)))))

(defn add-tool-volume
  [tool-uuid volume-map]
  (when-not (tool-has-settings? tool-uuid)
    (throw (Exception. (str "Tool " tool-uuid " does not have a container."))))
  (let [settings-uuid (tool-settings-uuid tool-uuid)]
    (dissoc
     (if-not (volume-mapping? settings-uuid (:host_path volume-map) (:container_path volume-map))
       (add-volume settings-uuid volume-map)
       (volume-mapping settings-uuid (:host_path volume-map) (:container_path volume-map)))
     :container_settings_id)))

(defn volume-field
  [tool-uuid volume-uuid field-kw]
  (let [fields (tool-volume tool-uuid volume-uuid)]
    (or (select-keys fields [field-kw]) nil)))

(defn tool-volumes-from
  "Returns a map with info about a particular container from which the tool's container will mount volumes."
  [tool-uuid volumes-from-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings-uuid (tool-settings-uuid tool-uuid)]
      (when (settings-has-volumes-from? settings-uuid volumes-from-uuid)
        (volumes-from-settings settings-uuid (:data_containers_id (volumes-from volumes-from-uuid)))))))

(defn- add-settings-volumes-from
  [settings-id vf-map]
  (transaction
    (let [data-container-id (or (find-data-container-id vf-map)
                                (:id (add-data-container vf-map)))]
      (when-not (volumes-from-mapping? settings-id data-container-id)
        (add-volumes-from settings-id data-container-id))
      (volumes-from-settings settings-id data-container-id))))

(defn add-tool-volumes-from
  [tool-uuid vf-map]
  (when-not (tool-has-settings? tool-uuid)
    (throw (Exception. (str "Tool " tool-uuid " does not have a container."))))
  (add-settings-volumes-from (tool-settings-uuid tool-uuid) vf-map))

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

(defn add-tool-container
  [tool-uuid info-map]
  (when (tool-has-settings? tool-uuid)
    (throw (Exception. (str "Tool " tool-uuid " already has container settings."))))
  (let [devices  (:container_devices info-map)
        volumes  (:container_volumes info-map)
        vfs      (:container_volumes_from info-map)
        settings (dissoc info-map :container_devices :container_volumes :container_volumes_from)
        info-map (assoc info-map :tools_id (uuidify tool-uuid))]
    (log/warn "adding container information for tool" tool-uuid ":" info-map)
    (transaction
     (let [img-id        (find-or-add-image-id (:image info-map))
           settings-map  (add-settings info-map)
           settings-uuid (:id settings-map)]
       (update-tool {:id tool-uuid :container_images_id img-id})
       (doseq [d devices]
         (add-device settings-uuid d))
       (doseq [v volumes]
         (add-volume settings-uuid v))
       (doseq [vf vfs]
         (add-settings-volumes-from settings-uuid vf))
       (tool-container-info tool-uuid)))))

(defn delete-tool-device
  [tool-uuid device-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings-uuid (tool-settings-uuid tool-uuid)]
      (when (settings-has-device? settings-uuid device-uuid)
        (log/warn "deleting device" device-uuid "from tool" tool-uuid)
        (delete-device device-uuid)
        nil))))

(defn delete-tool-volume
  [tool-uuid volume-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings-uuid (tool-settings-uuid tool-uuid)]
      (when (settings-has-volume? settings-uuid volume-uuid)
        (log/warn "deleting volume" volume-uuid "for tool" tool-uuid)
        (delete-volume volume-uuid)
        nil))))

(defn delete-tool-volumes-from
  [tool-uuid vf-uuid]
  (when (tool-has-settings? tool-uuid)
    (let [settings-uuid (tool-settings-uuid tool-uuid)]
      (when (settings-has-volumes-from? settings-uuid vf-uuid)
        (log/warn "deleting volumes-from" vf-uuid "for tool" tool-uuid)
        (delete-volumes-from vf-uuid)
        nil))))

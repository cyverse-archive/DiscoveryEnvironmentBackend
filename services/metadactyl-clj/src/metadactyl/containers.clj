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
  (select container-images
          (where {:id (uuidify image-id)})))

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
            (fn [k v]
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
     :cpu-shares - integer granting shares of the CPU to the container.
     :memory-limit - bigint number of bytes of RAM to give to the container.
     :network-mode - either bridge or none
     :working-directory - default working directory for the container
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

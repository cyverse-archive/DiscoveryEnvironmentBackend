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

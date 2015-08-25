(ns facepalm.c210-2015082502
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150825.02")

(defn- add-ncbri-container-images-and-data-containers
  []
  (let [configs-id (:id (insert :container_images
                     (values {:name "discoenv/ncbi-sra-configs"
                              :tag  "latest"
                              :url  "https://registry.hub.docker.com/u/discoenv/ncbi-sra-configs"})))
        ssh-key-id (:id (insert :container_images
                     (values {:name "gims.iplantcollaborative.org:5000/ncbi-ssh-key"
                              :tag  "latest"
                              :url  "https://gims.iplantcollaborative.org:5000/ncbi-ssh-key"})))]
    (insert :data_containers
      (values [{:name_prefix       "ncbi-sra-configs"
               :container_image_id configs-id
               :read_only          true}
              {:name_prefix        "ncbi-ssh-key"
               :container_image_id ssh-key-id
               :read_only          true}]))

    (insert :version (values {:version version}))))

(defn convert
  "Performs the conversion for database version 2.1.0:20150825.02"
  []
  (println "Performing the conversion for" version)
  (add-ncbri-container-images-and-data-containers))

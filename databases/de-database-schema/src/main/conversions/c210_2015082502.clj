(ns facepalm.c210-2015082502
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150825.02")

(defn- add-ncbi-container-images-and-data-containers
  []
  (let [configs-id (:id (insert :container_images
                     (values {:name "discoenv/ncbi-sra-configs"
                              :tag  "latest"
                              :url  "https://registry.hub.docker.com/u/discoenv/ncbi-sra-configs"})))
        test-configs-id (:id (insert :container_images
                               (values {:name "discoenv/ncbi-sra-configs"
                                        :tag  "test"
                                        :url  "https://registry.hub.docker.com/u/discoenv/ncbi-sra-configs"})))
        ssh-key-id (:id (insert :container_images
                          (values {:name "gims.iplantcollaborative.org:5000/ncbi-sra-submit-ssh-key-data"
                                   :tag  "latest"
                                   :url  "https://gims.iplantcollaborative.org:5000/ncbi-sra-submit-ssh-key-data"})))]
    (insert :data_containers
      (values [{:name_prefix         "ncbi-sra-configs"
                :container_images_id configs-id}
               {:name_prefix         "ncbi-sra-test-configs"
                :container_images_id test-configs-id}
               {:name_prefix         "ncbi-sra-submit-ssh-key-data"
                :container_images_id ssh-key-id}]))))

(defn convert
  "Performs the conversion for database version 2.1.0:20150825.02"
  []
  (println "Performing the conversion for" version)
  (add-ncbi-container-images-and-data-containers))

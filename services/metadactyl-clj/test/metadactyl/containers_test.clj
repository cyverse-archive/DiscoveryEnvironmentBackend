(ns metadactyl.containers-test
  (:use [clojure.test]
        [metadactyl.containers]
        [korma.core]
        [korma.db]
        [kameleon.entities])
  (:require [clojure.string :as string]))

;;; These tests assume that you have a clean instance of the de
;;; database running locally on port 5432. It's recommended that you
;;; use the de-db and de-db-loader images to get a database running
;;; with docker.


(defdb testdb (postgres {:db "de"
                         :user "de"
                         :password "notprod"
                         :host (System/getenv "POSTGRES_PORT_5432_TCP_ADDR")
                         :port (System/getenv "POSTGRES_PORT_5432_TCP_PORT")
                         :delimiters ""}))

(def image-info-map (add-image-info {:name "discoenv/de-db" :tag "latest" :url "https://www.google.com"}))


(deftest image-tests []
  (is (not (image? {:name "test" :tag "test"})))

  (is (image? {:name "discoenv/de-db" :tag "latest"}))

  (is (not (nil? (image-id {:name "discoenv/de-db" :tag "latest"}))))

  (is (= {:name "discoenv/de-db" :tag "latest" :url "https://www.google.com"}
         (dissoc (image-info (image-id {:name "discoenv/de-db" :tag "latest"})) :id))))


(def tool-map (first (select tools (where {:name "notreal"}))))

(def settings-map  (add-settings {:name "test"
                                  :cpu_shares 1024
                                  :memory_limit 2048
                                  :network_mode "bridge"
                                  :working_directory "/work"
                                  :tools_id (:id tool-map)}))

(deftest settings-tests []
  (is (not (nil? (:id settings-map))))

  (is (= {:name "test"
          :cpu_shares 1024
          :memory_limit 2048
          :network_mode "bridge"
          :working_directory "/work"
          :tools_id (:id tool-map)
          :entrypoint nil}
         (dissoc (settings (:id settings-map)) :id)))

  (is (settings? (:id settings-map)))

  (is (tool-has-settings? (:id tool-map))))

(def devices-map (add-device (:id settings-map) {:host_path "/dev/null" :container_path "/dev/yay"}))

(deftest devices-tests []
  (is (not (nil? (:id devices-map))))

  (is (= {:host_path "/dev/null" :container_path "/dev/yay" :container_settings_id (:id settings-map)}
         (dissoc (device (:id devices-map)) :id)))

  (is (device? (:id devices-map)))

  (is (device-mapping? (:id settings-map) "/dev/null" "/dev/yay"))

  (is (settings-has-device? (:id settings-map) (:id devices-map))))

(def volume-map (add-volume (:id settings-map) {:host_path "/tmp" :container_path "/foo"}))

(deftest volumes-tests []
  (is (not (nil? (:id volume-map))))

  (is (= {:host_path "/tmp" :container_path "/foo" :container_settings_id (:id settings-map)}
         (dissoc (volume (:id volume-map)) :id)))

  (is (volume? (:id volume-map)))

  (is (volume-mapping? (:id settings-map) "/tmp" "/foo"))

  (is (settings-has-volume? (:id settings-map) (:id volume-map))))

(def volumes-from-map (add-volumes-from (:id settings-map) "test-name"))

(defn volumes-from-test []
  (is (not (nil? (:id volumes-from-map))))

  (is (= {:name "test-name" :container_settings_id (:id settings-map)}
         (dissoc (volumes-from (:id volumes-from-map)) :id)))

  (is (volumes-from? (:id volumes-from-map)))

  (is (volumes-from-mapping? (:id settings-map) "test-name"))

  (is (settings-has-volumes-from? (:id settings-map) (:id volumes-from-map))))


(def updated-tool (update tools
                          (set-fields {:container_images_id (:id image-info-map)})
                          (where {:id (:id tool-map)})))

(defn updated-tool-tests []
  (is (not (nil? (:id updated-tool))))

  (is (= (dissoc image-info-map :id)
         (tool-image-info (:id updated-tool)))))

(ns metadactyl.containers-test
  (:use [clojure.test]
        [metadactyl.containers]
        [korma.core]
        [korma.db])
  (:require [clojure.string :as string]))

;;; These tests assume that you have a clean instance of the de
;;; database running locally on port 5432. It's recommended that you
;;; use the de-db and de-db-loader images to get a database running
;;; with docker.


(defdb testdb (postgres {:db "de"
                         :user "de"
                         :password "notprod"
                         :host "127.0.0.1"
                         :port "5432"
                         :delimiters ""}))

(def image-info-map (add-image-info {:name "discoenv/de-db" :tag "latest" :url "https://www.google.com"}))

(deftest image-tests []
  (is (not (image? {:name "test" :tag "test"})))
  
  (is (image? {:name "discoenv/de-db" :tag "latest"}))
  
  (is (not (nil? (image-id {:name "discoenv/de-db" :tag "latest"}))))
  
  (is (= {:name "discoenv/de-db" :tag "latest" :url "https://www.google.com"}
         (dissoc (image-info (image-id {:name "discoenv/de-db" :tag "latest"})) :id))))

(def settings-map  (add-settings {:name "test"
                                  :cpu_shares 1024
                                  :memory_limit 2048
                                  :network_mode "bridge"
                                  :working_directory "/work"}))

(deftest settings-tests []
  (is (not (nil? (:id settings-map))))

  (is (= {:name "test"
          :cpu_shares 1024
          :memory_limit 2048
          :network_mode "bridge"
          :working_directory "/work"}
         (dissoc (settings (:id settings-map)) :id)))

  (is (settings? (:id settings-map))))

(def devices-map (add-device (:id settings-map) "/dev/null" "/dev/yay"))

(deftest devices-tests []
  (is (not (nil? (:id devices-map))))

  (is (= {:host_path "/dev/null" :container_path "/dev/yay" :container_settings_id (:id settings-map)}
         (dissoc (device (:id devices-map)) :id)))

  (is (device? (:id devices-map)))

  (is (device-mapping? (:id settings-map) "/dev/null" "/dev/yay"))

  (is (settings-has-device? (:id settings-map))))

(def volume-map (add-volume (:id settings-map) "/tmp" "/foo"))

(deftest volumes-tests []
  (is (not (nil? (:id volume-map))))

  (is (= {:host_path "/tmp" :container_path "/foo" :container_settings_id (:id settings-map)}
         (dissoc (volume (:id volume-map)) :id)))

  (is (volume? (:id volume-map)))

  (is (volume-mapping? (:id settings-map) "/tmp" "/foo"))

  (is (settings-has-volume? (:id settings-map))))

(def volume-from-map (add-volume-from (:id settings-map) "test-name"))

(defn volumes-from-test []
  (is (not (nil? (:id volume-from-map))))

  (is (= {:name "test-name" :container_settings_id (:id settings-map)}
         (dissoc (volume-from (:id volume-from-map)) :id)))

  (is (volume-from? (:id volume-from-map)))

  (is (volume-from-mapping? (:id settings-map) "test-name"))

  (is (settings-has-volume-from? (:id settings-map))))

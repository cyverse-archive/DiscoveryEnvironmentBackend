(ns data-info.util.paths
  (:require [clojure-commons.file-utils :as ft]
            [clj-jargon.item-info :as item]
            [data-info.util.config :as cfg]))


(def IPCRESERVED "ipc-reserved-unit")
(def IPCSYSTEM "ipc-system-avu")


(defn super-user?
  [username]
  (.equals username (cfg/irods-user)))

(defn user-home-dir
  [user]
  (ft/path-join "/" (cfg/irods-zone) "home" user))


(defn base-trash-path
  []
  (item/trash-base-dir (cfg/irods-zone) (cfg/irods-user)))


(defn user-trash-path
  [user]
  (item/trash-base-dir (cfg/irods-zone) user))


(defn in-trash?
  [user fpath]
  (.startsWith fpath (user-trash-path user)))

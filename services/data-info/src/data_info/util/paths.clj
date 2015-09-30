(ns data-info.util.paths
  (:require [clojure-commons.file-utils :as ft]
            [clj-jargon.item-info :as item]
            [data-info.util.config :as cfg]))


(def IPCRESERVED "ipc-reserved-unit")
(def IPCSYSTEM "ipc-system-avu")


(defn ^Boolean super-user?
  [^String username]
  (.equals username (cfg/irods-user)))

(defn ^String user-home-dir
  [^String user]
  (ft/path-join (cfg/irods-home) user))


(defn ^String base-trash-path
  []
  (item/trash-base-dir (cfg/irods-zone) (cfg/irods-user)))


(defn ^String user-trash-path
  [^String user]
  (ft/path-join (base-trash-path) user))


(defn ^Boolean in-trash?
  [^String user ^String fpath]
  (.startsWith fpath (base-trash-path)))

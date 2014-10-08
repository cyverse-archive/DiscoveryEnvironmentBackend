(ns data-info.services.stat
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :refer [is-dir? stat]]
            [clj-jargon.metadata :refer [get-attribute]]
            [clj-jargon.permissions :refer [list-user-perms permission-for owns?]]
            [clojure-commons.file-utils :as ft]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.services.common-paths :as paths]
            [data-info.services.type-detect.irods :as filetypes]
            [data-info.services.validators :as validators])
  (:import [clojure.lang IPersistentMap]))


(defn- count-shares
  [cm user path]
  (let [filter-users (set (conj (cfg/perms-filter) user (cfg/irods-user)))
        other-perm?  (fn [perm] (not (contains? filter-users (:user perm))))]
    (count (filterv other-perm? (list-user-perms cm path)))))


(defn- merge-counts
  [stat-map cm user path]
  (if (is-dir? cm path)
    (assoc stat-map
      :file-count (icat/number-of-files-in-folder user (cfg/irods-zone) path)
      :dir-count  (icat/number-of-folders-in-folder user (cfg/irods-zone) path))
    stat-map))


(defn- merge-shares
  [stat-map cm user path]
  (if (owns? cm user path)
    (assoc stat-map :share-count (count-shares cm user path))
    stat-map))


(defn- merge-type-info
  [stat-map cm user path]
  (if-not (is-dir? cm path)
    (assoc stat-map
      :info-type (filetypes/get-types cm user path)
      :mime-type (filetypes/detect-media-type cm path))
    stat-map))


(defn ^IPersistentMap decorate-stat
  [^IPersistentMap cm ^String user ^IPersistentMap stat]
  (let [path (:path stat)]
    (-> stat
      (assoc :id         (-> (get-attribute cm path "ipc_UUID") first :value)
             :label      (paths/id->label user path)
             :permission (permission-for cm user path))
      (merge-type-info cm user path)
      (merge-shares cm user path)
      (merge-counts cm user path))))


(defn ^IPersistentMap path-stat
  [^IPersistentMap cm ^String user ^String path]
  (let [path (ft/rm-last-slash path)]
    (log/debug "[path-stat] user:" user "path:" path)
    (validators/path-exists cm path)
    (decorate-stat cm user (stat cm path))))


(defn do-stat
  [{user :user} {paths :paths}]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    {:paths (into {} (map #(vector % (path-stat cm user %)) paths))}))

(with-pre-hook! #'do-stat
  (fn [params body]
    (dul/log-call "do-stat" params body)
    (cv/validate-map params {:user string?})
    (cv/validate-map body {:paths vector?})
    (cv/validate-map body {:paths #(not (empty? %))})
    (cv/validate-map body {:paths #(every? (comp not string/blank?) %)})
    (validators/validate-num-paths (:paths body))))

(with-post-hook! #'do-stat (dul/log-func "do-stat"))

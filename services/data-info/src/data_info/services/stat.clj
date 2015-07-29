(ns data-info.services.stat
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [slingshot.slingshot :refer [throw+]]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.by-uuid :as uuid]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :as info]
            [clj-jargon.metadata :as meta]
            [clj-jargon.permissions :as perm]
            [clj-jargon.users :as users]
            [clojure-commons.error-codes :refer [ERR_DOES_NOT_EXIST ERR_NOT_A_USER ERR_NOT_READABLE]]
            [clojure-commons.file-utils :as ft]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.irods :as irods]
            [data-info.util.validators :as validators])
  (:import [clojure.lang IPersistentMap]))


(defn- get-types
  "Gets all of the filetypes associated with path."
  [cm user path]
  (when-not (info/exists? cm path)
    (throw+ {:error_code ERR_DOES_NOT_EXIST :path path}))
  (when-not (users/user-exists? cm user)
    (throw+ {:error_code ERR_NOT_A_USER :user user}))
  (when-not (perm/is-readable? cm user path)
    (throw+ {:error_code ERR_NOT_READABLE
             :user       user
             :path       path}))
  (let [path-types (meta/get-attribute cm path (cfg/type-detect-type-attribute))]
    (log/info "Retrieved types" path-types "from" path "for" (str user "."))
    (or (:value (first path-types) ""))))


(defn- count-shares
  [cm user path]
  (let [filter-users (set (conj (cfg/perms-filter) user (cfg/irods-user)))
        other-perm?  (fn [perm] (not (contains? filter-users (:user perm))))]
    (count (filterv other-perm? (perm/list-user-perms cm path)))))


(defn- merge-counts
  [stat-map cm user path]
  (if (info/is-dir? cm path)
    (assoc stat-map
      :file-count (icat/number-of-files-in-folder user (cfg/irods-zone) path)
      :dir-count  (icat/number-of-folders-in-folder user (cfg/irods-zone) path))
    stat-map))


(defn- merge-shares
  [stat-map cm user path]
  (if (perm/owns? cm user path)
    (assoc stat-map :share-count (count-shares cm user path))
    stat-map))


(defn- merge-type-info
  [stat-map cm user path]
  (if-not (info/is-dir? cm path)
    (assoc stat-map
      :infoType     (get-types cm user path)
      :content-type (irods/detect-media-type cm path))
    stat-map))


(defn ^IPersistentMap decorate-stat
  [^IPersistentMap cm ^String user ^IPersistentMap stat]
  (let [path (:path stat)]
    (-> stat
      (assoc :id         (-> (meta/get-attribute cm path uuid/uuid-attr) first :value)
             :permission (perm/permission-for cm user path))
      (merge-type-info cm user path)
      (merge-shares cm user path)
      (merge-counts cm user path))))


(defn ^IPersistentMap path-stat
  [^IPersistentMap cm ^String user ^String path]
  (let [path (ft/rm-last-slash path)]
    (log/debug "[path-stat] user:" user "path:" path)
    (validators/path-exists cm path)
    (decorate-stat cm user (info/stat cm path))))


(defn do-stat
  [{user :user} {paths :paths}]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    {:paths (into {} (map (juxt identity (partial path-stat cm user)) paths))}))

(with-pre-hook! #'do-stat
  (fn [params body]
    (dul/log-call "do-stat" params body)
    (cv/validate-map body {:paths vector?})
    (cv/validate-map body {:paths (complement empty?)})
    (cv/validate-map body {:paths (partial every? (comp not string/blank?))})
    (validators/validate-num-paths (:paths body))))

(with-post-hook! #'do-stat (dul/log-func "do-stat"))

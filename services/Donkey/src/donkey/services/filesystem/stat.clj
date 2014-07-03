(ns donkey.services.filesystem.stat
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.util.config]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [is-dir? stat]]
        [clj-jargon.item-ops :only [input-stream]]
        [clj-jargon.metadata :only [get-attribute]]
        [clj-jargon.permissions :only [list-user-perms permission-for owns?]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.services.filesystem.validators :as validators]
            [donkey.services.garnish.irods :as filetypes]
            [clj-icat-direct.icat :as icat])
  (:import [org.apache.tika Tika]))

(defn- count-shares
  [cm user path]
  (let [filter-users (set (conj (fs-perms-filter) user (irods-user)))
        full-listing (list-user-perms cm path)]
    (count
     (filterv
      #(not (contains? filter-users (:user %1)))
      full-listing))))

(defn- merge-counts
  [stat-map cm user path]
  (if (is-dir? cm path)
    (merge stat-map {:file-count (icat/number-of-files-in-folder user (irods-zone) path)
                     :dir-count  (icat/number-of-folders-in-folder user (irods-zone) path)})
    stat-map))

(defn- merge-shares
  [stat-map cm user path]
  (if (owns? cm user path)
    (merge stat-map {:share-count (count-shares cm user path)})
    stat-map))

(defn- merge-type-info
  [stat-map cm user path]
  (if-not (is-dir? cm path)
    (-> stat-map
      (merge {:info-type (filetypes/get-types cm user path)})
      (merge {:mime-type (.detect (Tika.) (ft/basename path))}))
    stat-map))

(defn path-is-dir?
  [path]
  (with-jargon (jargon-cfg) [cm]
    (validators/path-exists cm path)
    (is-dir? cm path)))

(defn decorate-stat
  [cm user stat]
  (let [path (:path stat)]
    (-> stat
        (assoc :id (:value (first (get-attribute cm path "ipc_UUID")))
               :label      (id->label cm user path)
               :permission (permission-for cm user path))
        (merge-type-info cm user path)
        (merge-shares cm user path)
        (merge-counts cm user path))))

(defn path-stat
  [cm user path]
  (let [path (ft/rm-last-slash path)]
    (log/warn "[path-stat] user:" user "path:" path)
    (validators/path-exists cm path)
    (decorate-stat cm user (stat cm path))))

(defn do-stat
  [{user :user} {paths :paths}]
  (with-jargon (jargon-cfg) [cm]
    {:paths (into {} (map #(vector % (path-stat cm user %)) paths))}))

(with-pre-hook! #'do-stat
  (fn [params body]
    (log-call "do-stat" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths vector?})
    (validate-map body {:paths #(not (empty? %1))})
    (validate-map body {:paths #(every? (comp not string/blank?) %1)})
    (validate-num-paths (:paths body))))

(with-post-hook! #'do-stat (log-func "do-stat"))

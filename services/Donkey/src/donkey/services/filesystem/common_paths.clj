(ns donkey.services.filesystem.common-paths
  (:use [clj-jargon.init]
        [clj-jargon.item-info])
  (:require [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [clojure.set :as set]
            [donkey.util.config :as cfg]))

(def IPCRESERVED "ipc-reserved-unit")
(def IPCSYSTEM "ipc-system-avu")

(defn trace-log
  [trace-type func-name namespace params]
  (let [log-ns (str "trace." namespace)
        desc   (str "[" trace-type "][" func-name "]")
        msg    (apply print-str desc params)]
    (log/log log-ns :trace nil msg)))

(defmacro log-call
  [func-name & params]
  `(trace-log "call" ~func-name ~*ns* [~@params]))

(defn log-func*
  [func-name namespace]
  (fn [result]
    (trace-log "result" func-name namespace result)))

(defmacro log-func
  [func-name]
  `(log-func* ~func-name ~*ns*))

(defmacro log-result
  [func-name & result]
  `(trace-log "result" ~func-name ~*ns* [~@result]))

(defn super-user?
  [username]
  (.equals username (cfg/irods-user)))

(defn user-home-dir
  [user]
  (ft/path-join "/" (cfg/irods-zone) "home" user))

(defn string-contains?
  [container-str str-to-check]
  (pos? (count (set/intersection (set (seq container-str)) (set (seq str-to-check))))))

(defn good-string?
  [str-to-check]
  (not (string-contains? (cfg/fs-filter-chars) str-to-check)))

(defn valid-file-map? [map-to-check] (good-string? (:id map-to-check)))

(defn valid-path? [path-to-check] (good-string? path-to-check))

(defn sharing?
  [abs]
  (= (ft/rm-last-slash (cfg/irods-home))
     (ft/rm-last-slash abs)))

(defn community? [abs] (= (cfg/fs-community-data) abs))

(defn base-trash-path
  []
     (with-jargon (cfg/jargon-cfg) [cm]
       (trash-base-dir cm)))

(defn user-trash-path
  ([user]
     (with-jargon (cfg/jargon-cfg) [cm]
       (user-trash-path cm user)))
  ([cm user]
     (trash-base-dir cm user)))

(defn user-trash-dir?
  ([user path-to-check]
     (with-jargon (cfg/jargon-cfg) [cm]
       (user-trash-dir? cm user path-to-check)))
  ([cm user path-to-check]
     (= (ft/rm-last-slash path-to-check)
        (ft/rm-last-slash (user-trash-path cm user)))))

(defn in-trash?
  [cm user fpath]
  (.startsWith fpath (user-trash-path cm user)))

(defn date-mod-from-stat
  [stat]
  (str (long (.. stat getModifiedAt getTime))))

(defn date-created-from-stat
  [stat]
  (str (long (.. stat getCreatedAt getTime))))

(defn size-from-stat
  [stat]
  (str (.getObjSize stat)))

(defn id->label
  "Generates a label given a listing ID (read as absolute path)."
  [cm user id]
  (cond
   (user-trash-dir? cm user id)
   "Trash"

   (sharing? (ft/add-trailing-slash id))
   "Shared With Me"

   (community? id)
   "Community Data"

   :else
   (ft/basename id)))

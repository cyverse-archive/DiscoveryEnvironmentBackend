(ns donkey.services.filesystem.common-paths
  (:require [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [clj-jargon.item-info :as item]
            [donkey.util.config :as cfg]
            [donkey.util.validators :as valid]))


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
  [^String username]
  (.equals username (cfg/irods-user)))

(defn user-home-dir
  [user]
  (ft/path-join "/" (cfg/irods-zone) "home" user))


(defn valid-path? [path-to-check] (valid/good-string? path-to-check))

(defn base-trash-path
  []
  (item/trash-base-dir (cfg/irods-zone) (cfg/irods-user)))


(defn user-trash-path
  [user]
  (ft/path-join (base-trash-path) user))

(defn in-trash?
  [user ^String fpath]
  (.startsWith fpath (user-trash-path user)))

(defn- dir-equal?
  [path comparison]
  (apply = (map ft/rm-last-slash [path comparison])))

(defn- user-trash-dir?
  [user abs]
  (dir-equal? abs (user-trash-path user)))
(defn- sharing? [abs] (dir-equal? abs (cfg/irods-home)))
(defn- community? [abs] (dir-equal? abs (cfg/fs-community-data)))

(defn id->label
  "Generates a label given a listing ID (read as absolute path)."
  [user id]
  (cond
    (user-trash-dir? user id)             "Trash"
    (sharing? id)                         "Shared With Me"
    (community? id)                       "Community Data"
    :else                                 (ft/basename id)))

(def path->label id->label)

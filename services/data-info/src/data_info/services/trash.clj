(ns data-info.services.trash
  (:use [clojure-commons.error-codes]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-ops]
        [clj-jargon.item-info]
        [clj-jargon.metadata]
        [clj-jargon.permissions]
        [clj-jargon.tickets]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.util.config :as cfg]
            [data-info.util.paths :as paths]
            [data-info.util.logging :as dul]
            [data-info.services.directory :as directory]
            [data-info.services.uuids :as uuids]
            [data-info.util.validators :as validators]))

(def alphanums (concat (range 48 58) (range 65 91) (range 97 123)))

(defn- rand-str
  [length]
  (apply str (take length (repeatedly #(char (rand-nth alphanums))))))

(defn- randomized-trash-path
  [user path-to-inc]
  (ft/path-join
   (paths/user-trash-path user)
   (str (ft/basename path-to-inc) "." (rand-str 7))))

(defn- move-to-trash
  [cm p user]
  (let [trash-path (randomized-trash-path user p)]
    (move cm p trash-path :user user :admin-users (cfg/irods-admins))
    (set-metadata cm trash-path "ipc-trash-origin" p paths/IPCSYSTEM)))

(defn- delete-paths
  [user paths]
  (let [home-matcher #(= (str "/" (cfg/irods-zone) "/home/" user)
                         (ft/rm-last-slash %1))]
    (with-jargon (cfg/jargon-cfg) [cm]
      (let [paths (mapv ft/rm-last-slash paths)]
        (validators/user-exists cm user)
        (validators/all-paths-exist cm paths)
        (validators/user-owns-paths cm user paths)

        ;;; Not allowed to delete the user's home directory.
        (when (some true? (mapv home-matcher paths))
          (throw+ {:error_code ERR_NOT_AUTHORIZED
                   :paths (filterv home-matcher paths)}))

        (doseq [p paths]
          (log/debug "path" p)
          (log/debug "readable?" user (owns? cm user p))

          ;;; Delete all of the tickets associated with the file.
          (let [path-tickets (mapv :ticket-id (ticket-ids-for-path cm (:username cm) p))]
            (doseq [path-ticket path-tickets]
              (delete-ticket cm (:username cm) path-ticket)))

          ;;; If the file isn't already in the user's trash, move it there
          ;;; otherwise, do a hard delete.
          (if-not (.startsWith p (paths/user-trash-path user))
            (move-to-trash cm p user)
            (delete cm p true))) ;;; Force a delete to bypass proxy user's trash.

         {:paths paths}))))

(defn- delete-uuid
  "Delete by UUID: given a user and a data item UUID, delete that data item, returning a list of filenames deleted."
  [user source-uuid]
  (let [path (ft/rm-last-slash (:path (uuids/path-for-uuid user source-uuid)))]
    (validators/validate-num-paths-under-folder user path)
    (delete-paths user [path])))

(defn- delete-uuid-contents
  "Delete contents by UUID: given a user and a data item UUID, delete the contents, returning a list of filenames deleted."
  [user source-uuid]
  (let [source (ft/rm-last-slash (:path (uuids/path-for-uuid user source-uuid)))]
    (with-jargon (cfg/jargon-cfg) [cm]
      (validators/validate-num-paths-under-folder user source)
      (validators/path-is-dir cm source))
    (let [paths (directory/get-paths-in-folder user source)]
      (delete-paths user paths))))

(defn- list-in-dir
  [cm fixed-path]
  (let [ffilter (proxy [java.io.FileFilter] [] (accept [stuff] true))]
    (.getListInDirWithFileFilter
      (:fileSystemAO cm)
      (file cm fixed-path)
      ffilter)))

(defn- delete-trash
  "Permanently delete the contents of a user's trash directory."
  [user]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [trash-dir  (paths/user-trash-path user)
          trash-list (mapv #(.getAbsolutePath %) (list-in-dir cm (ft/rm-last-slash trash-dir)))]
      (doseq [trash-path trash-list]
        (delete cm trash-path true))
      {:trash trash-dir
       :paths trash-list})))

(defn do-delete-uuid
  [{user :user} data-id]
  (delete-uuid user data-id))

(with-pre-hook! #'do-delete-uuid
  (fn [params data-id]
    (dul/log-call "do-delete-uuid" params data-id)

    (when (paths/super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED
               :user       (:user params)}))))

(with-post-hook! #'do-delete-uuid (dul/log-func "do-delete-uuid"))

(defn do-delete-uuid-contents
  [{user :user} data-id]
  (delete-uuid-contents user data-id))

(with-pre-hook! #'do-delete-uuid-contents
  (fn [params data-id]
    (dul/log-call "do-delete-uuid-contents" params data-id)

    (when (paths/super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED
               :user       (:user params)}))))

(with-post-hook! #'do-delete-uuid-contents (dul/log-func "do-delete-uuid-contents"))

(defn do-delete-trash
  [{user :user}]
  (delete-trash user))

(with-post-hook! #'do-delete-trash (dul/log-func "do-delete-trash"))

(with-pre-hook! #'do-delete-trash
  (fn [params]
    (dul/log-call "do-delete-trash" params)))

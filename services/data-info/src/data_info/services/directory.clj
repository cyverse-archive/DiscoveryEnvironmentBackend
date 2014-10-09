(ns data-info.services.directory
  (:require [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :as item]
            [clj-jargon.permissions :as perm]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.irods :as irods]
            [data-info.util.validators :as validators])
  (:import [clojure.lang ISeq]))


(defn ^ISeq get-paths-in-folder
  "Returns all of the paths of the members of a given folder that are visible to a given user.

   Parameters:
     user   - the username of the user
     folder - the folder to inspect
     limit  - (OPTIONAL) if provided, only the first <limit> members will be returned.

   Returns:
     It returns a list of paths."
  ([^String user ^String folder]
   (icat/folder-path-listing user (cfg/irods-zone) folder))

  ([^String user ^String folder ^Integer limit]
   (let [listing (icat/paged-folder-listing user (cfg/irods-zone) folder :full-path :asc limit 0)]
     (map :full_path listing))))


(defn- fmt-folder
  [{:keys [full_path modify_ts create_ts access_type_id uuid]}]
  {:id            uuid
   :path          full_path
   :permission    (perm/fmt-perm access_type_id)
   :date-created  (* (Integer/parseInt create_ts) 1000)
   :date-modified (* (Integer/parseInt modify_ts) 1000)})


(defn- list-directories
  "Lists the directories contained under path."
  [user path]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-readable cm user path)
    (validators/path-is-dir cm path)
    (let [stat (item/stat cm path)]
      {:id            (irods/lookup-uuid cm path)
       :path          path
       :permisssion   (perm/permission-for cm user path)
       :date-created  (:date-created stat)
       :date-modified (:date-modified stat)
       :folders       (map fmt-folder (icat/list-folders-in-folder user (cfg/irods-zone) path))})))


(defn do-directory
  [path-in-zone {user :user zone :zone}]
  {:folder (list-directories user (irods/abs-path zone path-in-zone))})

(with-pre-hook! #'do-directory
  (fn [path params]
    (dul/log-call "do-directory" path params)
    (cv/validate-map params {:user string?})))

(with-post-hook! #'do-directory (dul/log-func "do-directory"))

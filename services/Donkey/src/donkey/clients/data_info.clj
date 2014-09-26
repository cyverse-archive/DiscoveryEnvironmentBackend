(ns donkey.clients.data-info
  (:use [donkey.util.service :only [build-url-with-query]]
        [donkey.util.transformers :only [add-current-user-to-map]]
        [donkey.auth.user-attributes :only [current-user]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clj-jargon.init :as init]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.permissions :as perm]
            [clojure-commons.error-codes :as ce]
            [donkey.services.filesystem.common-paths :as cp]
            [donkey.services.filesystem.create :as cr]
            [donkey.services.filesystem.exists :as e]
            [donkey.services.filesystem.icat :as icat]
            [donkey.services.filesystem.metadata :as mt]
            [donkey.services.filesystem.sharing :as sharing]
            [donkey.services.filesystem.stat :as st]
            [donkey.services.filesystem.users :as users]
            [donkey.services.filesystem.uuids :as uuids])
  (:import [clojure.lang IPersistentMap ISeq]
           [java.util UUID]))


(defn ^Boolean irods-running?
  "Determines whether or not iRODS is running."
  []
  (try
    (init/with-jargon (icat/jargon-cfg) [cm]
      (item/exists? cm (:home cm)))
    (catch Exception e
      (log/error "Error performing iRODS status check:")
      (log/error (ce/format-exception e))
      false)))


(defn ^String user-home-folder
  "Determines the home folder for the given user.

   Parameters:
     user - the user of the home folder.

   Returns:
     It returns the absolute path to the home folder."
  [^String user]
  (cp/user-home-dir user))


(defn ^String base-trash-folder
  "It returns the root directory for all users' trash folders."
  []
  (cp/base-trash-path))


(defn ^String user-trash-folder
  "Determines the top-level trash folder for the given user.

   Parameters:
     user - the user of the trash folder.

   Returns:
     It returns the absolute path to the trash folder."
  [^String user]
  (cp/user-trash-path user))


(defn ensure-dir-created
  "If a folder doesn't exist, it creates the folder and makes the given user an owner of it.

   Parameters:
     user - the username of the user to become an owner of the new folder
     dir  - the absolute path to the folder"
  [^String user ^String dir]
  (init/with-jargon (icat/jargon-cfg) [cm]
    (when-not (item/exists? cm dir)
      (log/warn "creating" dir)
      (ops/mkdirs cm dir)
      (perm/set-owner cm dir user))))


(defn get-or-create-dir
  "Returns the path argument if the path exists and refers to a directory.  If
   the path exists and refers to a regular file then nil is returned.
   Otherwise, a new directory is created and the path is returned."
  [path]
  (log/debug "getting or creating dir: path =" path)
  (cond
   (not (e/path-exists? path))
   (cr/create (:shortUsername current-user) path)

   (and (e/path-exists? path) (st/path-is-dir? path))
   path

   (and (e/path-exists? path) (not (st/path-is-dir? path)))
   nil

   :else
   nil))

(defn gen-output-dir
  "Either obtains or creates a default output directory using a specified base name."
  [base]
  (first
   (remove #(nil? (get-or-create-dir %))
           (cons base (map #(str base "-" %) (iterate inc 1))))))

(defn build-path
  "Builds a path from a base path and a list of components."
  [path & components]
  (string/join
   "/"
   (cons (string/replace path #"/+$" "")
         (map #(string/replace % #"^/+|/+$" "") components))))


(defn get-tree-metaurl
  "Gets the URL used to get saved tree URLs."
  [user path]
  (->> (mt/metadata-get user path)
    (:metadata)
    (filter #(= (:attr %) "tree-urls"))
    (first)
    (:value)))


(defn save-tree-metaurl
  "Saves the URL used to get saved tree URLs. The metaurl argument should contain the URL used to
   obtain the tree URLs."
  [path metaurl]
  (mt/admin-metadata-set path {:attr "tree-urls" :value metaurl :unit ""}))


(defn ^ISeq list-user-groups
  "retrieves a list of groups names a given user belongs to

   Params:
     user - the username of the user of interest

   Returns:
     It returns the list of group names."
  [^String user]
  (users/list-user-groups user))


(defn ^IPersistentMap stat-by-uuid
  "Resolves a stat info for the entity with a given UUID.

   Params:
     user - the user requesting the info
     uuid - the UUID

   Returns:
     It returns a path-stat map containing an additional UUID field."
  [^String user ^UUID uuid]
  (uuids/path-for-uuid user uuid))


(defn ^ISeq stats-by-uuids-paged
  "Resolves the stat info for the entities with the given UUIDs. The results are paged.

   Params:
     user       - the user requesting the info
     sort-field - the stat field to sort on
     sort-order - the direction of the sort (asc|desc)
     limit      - the maximum number of results to return
     offset     - the number of results to skip before returning some
     uuids      - the UUIDS of interest

   Returns:
     It returns a page of stat info maps."
  [^String user ^String sort-field ^String sort-order ^Integer limit ^Integer offset ^ISeq uuids]
  (uuids/paths-for-uuids-paged user sort-field sort-order limit offset uuids))


(defn ^Boolean uuid-accessible?
  "Indicates if a filesystem entry is readble by a given user.

   Parameters:
     user     - the authenticated name of the user
     entry-id - the UUID of the filesystem entry

   Returns:
     It returns true if the user can access the entry, otherwise false"
  [^String user ^UUID entry-id]
  (uuids/uuid-accessible? user entry-id))


(defn ^Boolean owns?
  "Indicates if a file or folder is owned by a given user.

   Parameters:
     user       - the username of the user
     entry-path - The absolute path to the file or folder

   Returns:
     It returns true if the user own the entry, otherwise false."
  [^String user ^String entry-path]
  (init/with-jargon (icat/jargon-cfg) [cm]
    (perm/owns? cm user entry-path)))


(defn ^IPersistentMap share
  "grants access to a list of data entities for a list of users by a user

   Params:
     user        - the username of the sharing user
     share-withs - the list of usernames receiving access
     fpaths      - the list of absolute paths to the data entities being shared
     perm        - the permission being granted to the user users (read|write|own)

   Returns:
     It returns a map with the following fields:

       :user    - the list of users who actually received access
       :path    - the list of paths actually shared
       :skipped - the list of records for the things skipped, each record has the following fields:
                    :user   - the user who didn't get access
                    :path   - the path the user didn't get access to
                    :reason - the reason access wasn't granted
       :perm    - the permission that was granted"
  [^String user ^ISeq share-withs ^ISeq fpaths ^String perm]
  (sharing/share user share-withs fpaths perm))


(defn unshare
  "Params:
     user          - the username of the user removing access
     unshare-withs - the list of usernames having access removed
     fpaths        - the list of absolute paths ot the data entities losing accessibility

   Returns:
     It returns a map with the following fields:

       :user    - the list of users who lost access
       :path    - the list of paths that lost accessibility
       :skipped - a list of records for the things skipped, each record has the following fields:
                    :user   - the user who kept access
                    :path   - the path the user still can access
                    :reason - the reason access wasn't removed"
  [^String user ^ISeq unshare-withs ^ISeq fpaths]
  (sharing/unshare user unshare-withs fpaths))



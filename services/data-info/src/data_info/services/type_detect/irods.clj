(ns data-info.services.type-detect.irods
  (:use [clojure-commons.error-codes])
  (:require [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+]]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :as info]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.metadata :as meta]
            [clj-jargon.permissions :as perm]
            [clj-jargon.users :as users]
            [clojure-commons.file-utils :as ft]
            [heuristomancer.core :as hm]
            [data-info.util.config :as cfg]))


(defn get-file-type
  [cm path]
  "Uses heuristomancer to determine a the file type of a file."
  (let [result (hm/identify (ops/input-stream cm path) (cfg/filetype-read-amount))]
    (if-not (nil? result)
      (name result)
      result)))

(defn content-type
  "Determines the filetype of path. Reads in a chunk, writes it to a temp file, runs it
   against the configured script. If the script can't identify it, it's passed to Tika."
  [cm path]
  (log/trace "in content-type")

  (let [script-type (get-file-type cm path)]
    (log/info "Path " path " has a type of " script-type " from the script.")
    (if (or (nil? script-type) (empty? script-type))
      ""
      script-type)))

(defn add-type
  "Adds the type to a file in iRODS at path for the specified user."
  ([user path type]
    (with-jargon (cfg/jargon-cfg) [cm]
      (add-type cm user path type)))

  ([cm user path type]
    (when-not (info/exists? cm path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST :path path}))
    (when-not (users/user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER :user user}))
    (when-not (perm/is-writeable? cm user path)
      (throw+ {:error_code ERR_NOT_OWNER
               :user user
               :path path}))
    (meta/set-metadata cm path (cfg/type-detect-type-attribute) type "")
    (log/debug "Added type" type "to" path "for" (str user "."))
    {:path path
     :type type}))

(defn auto-add-type
  "Uses (content-type) to guess at a file type and associates it with the file."
  ([user path]
    (with-jargon (cfg/jargon-cfg) [cm]
      (auto-add-type cm user path)))

  ([cm user path]
    (when-not (info/exists? cm path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST :path path}))
    (when-not (users/user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER :user user}))
    (when-not (perm/is-writeable? cm user path)
      (throw+ {:error_code ERR_NOT_OWNER
               :user user
               :path path}))

    (let [type (content-type cm path)]
      (meta/add-metadata cm path (cfg/type-detect-type-attribute) type "")
      (log/debug "Auto-added type" type "to" path "for" (str user "."))
      {:path path
       :type type})))


(defn delete-type
  "Removes the association of type with path for the specified user."
  [user path type]
  (log/info "in delete-type")

  (with-jargon (cfg/jargon-cfg) [cm]
    (when-not (info/exists? cm path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST :path path}))
    (when-not (users/user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER :user user}))
    (when-not (perm/is-writeable? cm user path)
      (throw+ {:error_code ERR_NOT_OWNER
               :user user
               :path path}))
    (meta/delete-avus cm path (meta/get-attribute-value cm path (cfg/type-detect-type-attribute) type))
    (log/debug "Deleted type" type "from" path "for" (str user "."))
    {:path path
     :type type
     :user user}))


(defn get-types
  "Gets all of the filetypes associated with path."
  ([cm user path]
    (when-not (info/exists? cm path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST :path path}))
    (when-not (users/user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER :user user}))
    (when-not (perm/is-readable? cm user path)
      (throw+ {:error_code ERR_NOT_READABLE
               :user user
               :path path}))
    (let [path-types (meta/get-attribute cm path (cfg/type-detect-type-attribute))]
      (log/info "Retrieved types" path-types "from" path "for" (str user "."))
      (or (:value (first path-types) ""))))

  ([user path]
    (with-jargon (cfg/jargon-cfg) [cm]
      (get-types cm user path))))

(defn home-dir
  "Returns the path to the user's home directory."
  [cm user]
  (log/info "in home-dir")
  (ft/path-join "/" (:zone cm) "home" user))

(defn find-paths-with-type
  "Returns all of the paths under the user's home directory that have the specified type
   associated with it."
  [user type]
  (log/info "in find-paths-with-type")

  (with-jargon (cfg/jargon-cfg) [cm]
    (when-not (users/user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER :user user}))
    (let [paths-with-type (meta/list-everything-in-tree-with-attr cm
                                                                  (home-dir cm user)
                                                                  {:name (cfg/type-detect-type-attribute) :value type})]
      (log/debug "Looked up all paths with a type of" type "for" user "\n" paths-with-type)
      paths-with-type)))

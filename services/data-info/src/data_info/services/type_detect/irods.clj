(ns data-info.services.type-detect.irods
  (:use [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [exists?]]
        [clj-jargon.item-ops :only [input-stream]]
        [clj-jargon.metadata]
        [clj-jargon.permissions]
        [clj-jargon.users :only [user-exists?]]
        [clj-jargon.validations]
        [clojure-commons.error-codes]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as json]
            [heuristomancer.core :as hm]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [data-info.util.config :as cfg]))


(defn get-file-type
  [cm path]
  "Uses heuristomancer to determine a the file type of a file."
  (let [result (hm/identify (input-stream cm path) (cfg/filetype-read-amount))]
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
    (log/info "in add-type")

    (when-not (exists? cm path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST
               :path path}))

    (when-not (user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER
               :user user}))

    (when-not (is-writeable? cm user path)
      (throw+ {:error_code ERR_NOT_OWNER
               :user user
               :path path}))
    (set-metadata cm path (cfg/type-detect-type-attribute) type "")
    (log/info "Added type " type " to " path " for " user ".")
    {:path path
     :type type}))

(defn auto-add-type
  "Uses (content-type) to guess at a file type and associates it with the file."
  ([user path]
    (with-jargon (cfg/jargon-cfg) [cm]
      (auto-add-type cm user path)))

  ([cm user path]
    (log/info "in auto-add-type")

    (when-not (exists? cm path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST
               :path path}))

    (when-not (user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER
               :user user}))

    (when-not (is-writeable? cm user path)
      (throw+ {:error_code ERR_NOT_OWNER
               :user user
               :path path}))

    (let [type (content-type cm path)]
      (add-metadata cm path (cfg/type-detect-type-attribute) type "")
      (log/info "Auto-added type " type " to " path " for " user ".")
      {:path path
       :type type})))

(defn preview-auto-type
  "Returns the auto-type that (auto-add-type) would have associated with the file."
  [user path]
  (log/info "in preview-auto-type")

  (with-jargon (cfg/jargon-cfg) [cm]
    (when-not (exists? cm path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST
               :path path}))

    (when-not (user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER
               :user user}))

    (when-not (owns? cm user path)
      (throw+ {:error_code ERR_NOT_OWNER
               :user user
               :path path}))

    (let [ct (content-type cm path)]
      (log/info "Preview type of " path " for " user " is " ct ".")
      {:path path
       :type ct})))

(defn get-avus
  "Returns a list of avu maps for set of attributes associated with dir-path"
  [cm dir-path attr val]
  (log/info "in get-avus")

  (validate-path-lengths dir-path)
  (filter
    #(and (= (:attr %1) attr)
          (= (:value %1) val))
    (get-metadata cm dir-path)))

(defn delete-type
  "Removes the association of type with path for the specified user."
  [user path type]
  (log/info "in delete-type")

  (with-jargon (cfg/jargon-cfg) [cm]
    (when-not (exists? cm path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST
               :path path}))

    (when-not (user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER
               :user user}))

    (when-not (is-writeable? cm user path)
      (throw+ {:error_code ERR_NOT_OWNER
               :user user
               :path path}))
    (delete-avus cm path (get-avus cm path (cfg/type-detect-type-attribute) type))
    (log/info "Deleted type " type " from " path " for " user ".")
    {:path path
     :type type
     :user user}))

(defn unset-types
  "Removes all info-type associations from a path."
  [user path]
  (log/info "in unset-type")

  (with-jargon (cfg/jargon-cfg) [cm]
    (when-not (exists? cm path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST
               :path path}))

    (when-not (user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER
               :user user}))

    (when-not (is-writeable? cm user path)
      (throw+ {:error_code ERR_NOT_OWNER
               :user user
               :path path}))
    (delete-metadata cm path (cfg/type-detect-type-attribute))
    (log/info "Deleted types from" path "for" user)
    {:path path :user user}))

(defn get-types
  "Gets all of the filetypes associated with path."
  ([cm user path]
    (log/info "in get-types")

    (when-not (exists? cm path)
      (throw+ {:error_code ERR_DOES_NOT_EXIST
               :path path}))

    (when-not (user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER
               :user user}))

    (when-not (is-readable? cm user path)
      (throw+ {:error_code ERR_NOT_READABLE
               :user user
               :path path}))
    (let [path-types (get-attribute cm path (cfg/type-detect-type-attribute))]
      (log/info "Retrieved types " path-types " from " path " for " user ".")
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
    (when-not (user-exists? cm user)
      (throw+ {:error_code ERR_NOT_A_USER
               :user       user}))

    (let [paths-with-type (list-everything-in-tree-with-attr cm
                                                             (home-dir cm user)
                                                             {:name  (cfg/type-detect-type-attribute)
                                                              :value type})]
      (log/info "Looked up all paths with a type of " type " for " user "\n" paths-with-type)
      paths-with-type)))

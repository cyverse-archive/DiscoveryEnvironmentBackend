(ns donkey.services.filesystem.garnish.irods
  (:use [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [exists?]]
        [clj-jargon.metadata]
        [clj-jargon.permissions]
        [clj-jargon.users :only [user-exists?]]
        [clojure-commons.error-codes]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [donkey.util.config :as cfg]
            [donkey.services.filesystem.icat :as icat])
  (:import [org.apache.tika Tika]))

(defn add-type
  "Adds the type to a file in iRODS at path for the specified user."
  ([user path type]
    (with-jargon (icat/jargon-cfg) [cm]
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
    (set-metadata cm path (cfg/garnish-type-attribute) type "")
    (log/info "Added type " type " to " path " for " user ".")
    {:path path
     :type type}))

(defn unset-types
  "Removes all info-type associations from a path."
  [user path]
  (log/info "in unset-type")

  (with-jargon (icat/jargon-cfg) [cm]
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
    (delete-metadata cm path (cfg/garnish-type-attribute))
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
    (let [path-types (get-attribute cm path (cfg/garnish-type-attribute))]
      (log/info "Retrieved types " path-types " from " path " for " user ".")
      (or (:value (first path-types) ""))))

  ([user path]
    (with-jargon (icat/jargon-cfg) [cm]
      (get-types cm user path))))

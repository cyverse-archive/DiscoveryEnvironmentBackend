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

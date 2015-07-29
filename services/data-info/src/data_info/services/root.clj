(ns data-info.services.root
  (:use [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [exists?]]
        [clj-jargon.permissions :only [set-permission owns?]])
  (:require [clojure.tools.logging :as log]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [clojure-commons.file-utils :as ft]
            [data-info.services.stat :as stat]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.paths :as paths]
            [data-info.util.validators :as validators]
            [dire.core :refer [with-pre-hook! with-post-hook!]]))

(defn- get-root
  [cm user root-path]
  (-> (stat/path-stat cm user root-path)
      (select-keys [:id :path :date-created :date-modified :permission])))

(defn- get-trash-root
  [cm user]
  (let [trash-path (paths/user-trash-path user)]
    (when (not (exists? cm trash-path))
      (log/info "[get-trash-root] Creating" trash-path "for" user)
      (ops/mkdirs cm trash-path)
      (log/info "[get-trash-root] Setting own perms on" trash-path "for" user)
      (set-permission cm user trash-path :own))

    (when (not (owns? cm user trash-path))
      (log/info "[get-trash-root] Setting own perms on" trash-path "for" user)
      (set-permission cm user trash-path :own))

    (get-root cm user trash-path)))

(defn root-listing
  [user]
  (let [uhome          (paths/user-home-dir user)
        community-data (ft/rm-last-slash (cfg/community-data))
        irods-home     (ft/rm-last-slash (cfg/irods-home))]
    (log/debug "[root-listing]" "for" user)
    (with-jargon (cfg/jargon-cfg) [cm]
      (validators/user-exists cm user)
      {:roots (remove nil?
                [(get-root cm user uhome)
                 (get-root cm user community-data)
                 (get-root cm user irods-home)
                 (get-trash-root cm user)])})))

(defn do-root-listing
  [user]
  (root-listing user))

(with-pre-hook! #'do-root-listing
  (fn [user]
    (dul/log-call "do-root-listing" user)))

(with-post-hook! #'do-root-listing (dul/log-func "do-root-listing"))

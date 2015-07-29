(ns data-info.services.root
  (:use [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [exists?]]
        [clj-jargon.permissions :only [set-permission owns? permission-for]])
  (:require [clojure.tools.logging :as log]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [clojure-commons.file-utils :as ft]
            [data-info.util.config :as cfg]
            [data-info.util.irods :as irods]
            [data-info.util.logging :as dul]
            [data-info.util.paths :as paths]
            [data-info.util.validators :as validators]
            [dire.core :refer [with-pre-hook! with-post-hook!]]))

(defn- create-trash-folder?
  [cm user root-path]
  (and (= root-path (paths/user-trash-path user)) (not (exists? cm root-path))))

(defn root-listing
  ([user root-path]
    (root-listing user root-path false))

  ([user root-path set-own?]
    (let [root-path (ft/rm-last-slash root-path)]
      (log/info "[root-listing]" "for" user "at" root-path "with set own as" set-own?)
      (with-jargon (cfg/jargon-cfg) [cm]
        (log/info "in (root-listing)")
        (validators/user-exists cm user)

        (when (create-trash-folder? cm user root-path)
          (log/info "[root-listing] Creating" root-path "for" user)
          (ops/mkdirs cm root-path)
          (log/info "[root-listing] Setting own perms on" root-path "for" user)
          (set-permission cm user root-path :own))

        (validators/path-exists cm root-path)

        (when (and set-own? (not (owns? cm user root-path)))
          (log/info "[root-listing] set-own? is true and" root-path "is not owned by" user)
          (log/info "[root-listing] Setting own perms on" root-path "for" user)
          (set-permission cm user root-path :own))

        (-> (item/stat cm root-path)
            (select-keys [:path :date-created :date-modified])
            (assoc :id         (irods/lookup-uuid cm root-path)
                   :permission (permission-for cm user root-path)))))))

(defn do-root-listing
  [user]
  (let [uhome          (ft/path-join (cfg/irods-home) user)
        user-root-list (partial root-listing user)
        user-trash-dir (paths/user-trash-path user)]
    {:roots
     (remove
       nil?
       [(user-root-list uhome)
        (user-root-list (cfg/community-data))
        (user-root-list (cfg/irods-home))
        (user-root-list user-trash-dir true)])}))

(with-pre-hook! #'do-root-listing
  (fn [user]
    (dul/log-call "do-root-listing" user)))

(with-post-hook! #'do-root-listing (dul/log-func "do-root-listing"))

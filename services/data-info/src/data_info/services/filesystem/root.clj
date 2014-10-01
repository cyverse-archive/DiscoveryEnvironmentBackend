(ns data-info.services.filesystem.root
  (:use [clojure-commons.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [exists?]]
        [clj-jargon.item-ops :only [mkdir]]
        [clj-jargon.permissions :only [set-permission owns?]]
        [clj-jargon.listings :only [list-dir]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.util.config :as cfg]
            [data-info.services.filesystem.common-paths :as paths]
            [data-info.services.filesystem.icat :as icat]
            [data-info.services.filesystem.validators :as validators]))


(defn- create-trash-folder?
  [cm user root-path]
  (and (= root-path (paths/user-trash-path user)) (not (exists? cm root-path))))

(defn root-listing
  ([user root-path]
    (root-listing user root-path false))

  ([user root-path set-own?]
    (let [root-path (ft/rm-last-slash root-path)]
      (log/warn "[root-listing]" "for" user "at" root-path "with set own as" set-own?)
      (with-jargon (icat/jargon-cfg) [cm]
        (log/warn "in (root-listing)")
        (validators/user-exists cm user)

        (when (create-trash-folder? cm user root-path)
          (log/warn "[root-listing] Creating" root-path "for" user)
          (mkdir cm root-path)
          (log/warn "[root-listing] Setting own perms on" root-path "for" user)
          (set-permission cm user root-path :own))

        (validators/path-exists cm root-path)

        (when (and set-own? (not (owns? cm user root-path)))
          (log/warn "[root-listing] set-own? is true and" root-path "is not owned by" user)
          (log/warn "[root-listing] Setting own perms on" root-path "for" user)
          (set-permission cm user root-path :own))

        (when-let [res (list-dir cm user root-path :include-subdirs false)]
          (assoc res
                 :label (paths/id->label user (:id res))
                 :path  (:id res)
                 :id    (str "/root" (:id res))))))))


(defn do-root-listing
  [{user :user}]
  (let [uhome          (ft/path-join (cfg/irods-home) user)
        user-root-list (partial root-listing user)
        user-trash-dir (paths/user-trash-path user)]
    {:roots (remove nil? [(user-root-list uhome)
                          (user-root-list (cfg/community-data))
                          (user-root-list (cfg/irods-home))
                          (user-root-list user-trash-dir true)])}))


(with-pre-hook! #'do-root-listing
  (fn [params]
    (paths/log-call "do-root-listing" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-root-listing (paths/log-func "do-root-listing"))

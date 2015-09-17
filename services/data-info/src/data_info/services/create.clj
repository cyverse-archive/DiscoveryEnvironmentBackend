(ns data-info.services.create
  (:use [clojure-commons.error-codes]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.permissions :only [set-owner]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.validators :as validators]
            [data-info.util.paths :as paths]))

(defn- sort-existing
  [cm path]
  (when-not (validators/good-string? path)
    (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
             :path path}))
  (validators/path-not-exists cm path)

  (let [path-stack (take-while (complement nil?) (iterate ft/dirname path))
        ;; Using group-by rather than (juxt filter remove) to force list evaluation so set-owner
        ;; will be called correctly in the doseq below...
        {existing-paths true paths-to-mk false} (group-by (partial item/exists? cm) path-stack)
        target-dir (first existing-paths)]

    (when-not target-dir
      (throw+ {:error_code ERR_DOES_NOT_EXIST
               :path (last paths-to-mk)}))

    [(first existing-paths) paths-to-mk]))

(defn- create
  "Creates directories for the given paths on behalf of a user. The user becomes the owner of the
   new directories."
  [user paths]
  (log/debug (str "create " user " " paths))
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/validate-num-paths paths)
    (validators/user-exists cm user)
    (let [paths (set (map ft/rm-last-slash paths))
          sorted-paths (map (partial sort-existing cm) paths)
          target-dirs (set (map first sorted-paths))
          set-own-paths (set (map (comp last second) sorted-paths))
          paths-to-mk (sort (set (mapcat second sorted-paths)))]
      (doseq [target-dir target-dirs]
        (validators/path-writeable cm user target-dir))
      (doseq [path paths]
        ;; Don't attempt to create the parent of a dir that was already created in this request.
        (when-not (item/exists? cm path)
          (ops/mkdirs cm path)))
      (doseq [new-parent-dir set-own-paths]
        (set-owner cm new-parent-dir user))
      {:paths paths-to-mk})))

(defn do-create
  "Entrypoint for the API that calls (create)."
  [{user :user} {paths :paths}]
  (create user paths))

(with-pre-hook! #'do-create
  (fn [params body]
    (dul/log-call "do-create" params body)
    (log/info "Body: " body)
    (when (paths/super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED :user (:user params)}))))

(with-post-hook! #'do-create (dul/log-func "do-create"))

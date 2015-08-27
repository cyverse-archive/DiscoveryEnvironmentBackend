(ns data-info.services.rename
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-ops :only [move]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.services.uuids :as uuids]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.paths :as paths]
            [data-info.util.validators :as validators]))

(defn- rename-path
  "Data item renaming. As 'user', move 'source' to 'dest'."
  [user source dest]
  (with-jargon (cfg/jargon-cfg) [cm]
    (let [source    (ft/rm-last-slash source)
          dest      (ft/rm-last-slash dest)
          src-base  (ft/basename source)
          dest-base (ft/basename dest)]
      (if (= source dest)
        {:source source :dest dest :user user}
        (do
          (validators/user-exists cm user)
          (validators/path-exists cm source)
          (validators/path-is-dir cm (ft/dirname dest))
          (validators/user-owns-path cm user source)
          (validators/path-not-exists cm dest)

          (let [result (move cm source dest :user user :admin-users (cfg/irods-admins))]
            (when-not (nil? result)
              (throw+ {:error_code ERR_INCOMPLETE_RENAME
                       :paths result
                       :user user}))
            {:source source :dest dest :user user}))))))

(defn- rename-uuid
  "Rename by UUID: given a user, a source file UUID, and a new name, rename within the same folder."
  [user source-uuid dest-base]
  (let [source (ft/rm-last-slash (:path (uuids/path-for-uuid user source-uuid)))
        src-dir (ft/dirname source)
        dest (str (ft/add-trailing-slash src-dir) dest-base)]
    (validators/validate-num-paths-under-folder user source)
    (rename-path user source dest)))

(defn do-rename-uuid
  [{user :user} {dest-base :filename} source-uuid]
  (rename-uuid user source-uuid dest-base))

(defn- move-uuid
  "Rename by UUID: given a user, a source file UUID, and a new directory, move retaining the filename."
  [user source-uuid dest-dir]
  (let [source (ft/rm-last-slash (:path (uuids/path-for-uuid user source-uuid)))
        src-base (ft/basename source)
        dest (str (ft/add-trailing-slash dest-dir) src-base)]
    (validators/validate-num-paths-under-folder user source)
    (rename-path user source dest)))

(defn do-move-uuid
  [{user :user} {dest-dir :dirname} source-uuid]
  (rename-uuid user source-uuid dest-dir))

(with-post-hook! #'do-rename-uuid (dul/log-func "do-rename-uuid"))

(with-pre-hook! #'do-rename-uuid
  (fn [params body source-uuid]
    (dul/log-call "do-rename-uuid" params body source-uuid)
    (when (paths/super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED
               :user       (:user params)}))))

(with-post-hook! #'do-move-uuid (dul/log-func "do-move-uuid"))

(with-pre-hook! #'do-move-uuid
  (fn [params body source-uuid]
    (dul/log-call "do-move-uuid" params body source-uuid)
    (when (paths/super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED
               :user       (:user params)}))))

(ns donkey.services.filesystem.trash
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-ops]
        [clj-jargon.item-info]
        [clj-jargon.metadata]
        [clj-jargon.permissions]
        [clj-jargon.tickets]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.util.config :as cfg]
            [donkey.services.filesystem.common-paths :as paths]
            [donkey.services.filesystem.directory :as directory]
            [donkey.services.filesystem.icat :as jargon]
            [donkey.services.filesystem.validators :as validators]))

(defn- trash-origin-path
  [cm user p]
  (if (attribute? cm p "ipc-trash-origin")
    (:value (first (get-attribute cm p "ipc-trash-origin")))
    (ft/path-join (paths/user-home-dir user) (ft/basename p))))


(defn- restore-to-homedir?
  [cm p]
  (not (attribute? cm p "ipc-trash-origin")))

(defn- restoration-path
  [cm user path]
  (let [user-home   (paths/user-home-dir user)
        origin-path (trash-origin-path cm user path)
        inc-path    #(str origin-path "." %)]
    (if-not (exists? cm origin-path)
      origin-path
      (loop [attempts 0]
        (if (exists? cm (inc-path attempts))
          (recur (inc attempts))
          (inc-path attempts))))))

(defn- restore-parent-dirs
  [cm user path]
  (log/warn "restore-parent-dirs" (ft/dirname path))

  (when-not (exists? cm (ft/dirname path))
    (mkdirs cm (ft/dirname path))
    (log/warn "Created " (ft/dirname path))

    (loop [parent (ft/dirname path)]
      (log/warn "restoring path" parent)
      (log/warn "user parent path" user)

      (when (and (not= parent (paths/user-home-dir user)) (not (owns? cm user parent)))
        (log/warn (str "Restoring ownership of parent dir: " parent))
        (set-owner cm parent user)
        (recur (ft/dirname parent))))))

(defn- restore-path
  [{:keys [user paths user-trash]}]
  (with-jargon (jargon/jargon-cfg) [cm]
    (let [paths (mapv ft/rm-last-slash paths)]
      (validators/user-exists cm user)
      (validators/all-paths-exist cm paths)
      (validators/all-paths-writeable cm user paths)

      (let [retval (atom (hash-map))]
        (doseq [path paths]
          (let [fully-restored      (ft/rm-last-slash (restoration-path cm user path))
                restored-to-homedir (restore-to-homedir? cm path)]
            (log/warn "Restoring " path " to " fully-restored)

            (validators/path-not-exists cm fully-restored)
            (log/warn fully-restored " does not exist. That's good.")

            (restore-parent-dirs cm user fully-restored)
            (log/warn "Done restoring parent dirs for " fully-restored)

            (validators/path-writeable cm user (ft/dirname fully-restored))
            (log/warn fully-restored "is writeable. That's good.")

            (log/warn "Moving " path " to " fully-restored)
            (validators/path-not-exists cm fully-restored)

            (log/warn fully-restored " does not exist. That's good.")
            (move cm path fully-restored :user user :admin-users (cfg/irods-admins))
            (log/warn "Done moving " path " to " fully-restored)

            (reset! retval
                    (assoc @retval path {:restored-path fully-restored
                                         :partial-restore restored-to-homedir}))))
        {:restored @retval}))))

(defn do-restore
  [{user :user} {paths :paths}]
  (restore-path
    {:user  user
     :paths paths
     :user-trash (paths/user-trash-path user)}))

(with-post-hook! #'do-restore (paths/log-func "do-restore"))

(with-pre-hook! #'do-restore
  (fn [params body]
    (paths/log-call "do-restore" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential?})
    (validators/validate-num-paths-under-paths (:user params) (:paths body))))

(defn do-restore-all
  [{user :user}]
  (let [trash (paths/user-trash-path user)]
    (restore-path
      {:user       user
       :paths      (directory/get-paths-in-folder user trash)
       :user-trash trash})))

(with-pre-hook! #'do-restore-all
  (fn [params]
    (paths/log-call "do-restore-all" params)
    (validate-map params {:user string?})

    (let [user (:user params)]
      (when (paths/super-user? user)
        (throw+ {:error_code ERR_NOT_AUTHORIZED
                 :user       user}))
      (validators/validate-num-paths-under-folder user (paths/user-trash-path user)))))

(with-post-hook! #'do-restore-all (paths/log-func "do-restore-all"))

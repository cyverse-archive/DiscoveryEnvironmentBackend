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
            [clj-icat-direct.icat :as icat]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.util.config :as cfg]
            [donkey.services.filesystem.common-paths :as paths]
            [donkey.services.filesystem.directory :as directory]
            [donkey.services.filesystem.icat :as jargon]
            [donkey.services.filesystem.validators :as validators]))

(def alphanums (concat (range 48 58) (range 65 91) (range 97 123)))

(defn- trim-leading-slash
  [str-to-trim]
  (string/replace-first str-to-trim #"^\/" ""))

(defn- rand-str
  [length]
  (apply str (take length (repeatedly #(char (rand-nth alphanums))))))

(defn- randomized-trash-path
  [user path-to-inc]
  (ft/path-join
   (paths/user-trash-path user)
   (str (ft/basename path-to-inc) "." (rand-str 7))))

(defn- move-to-trash
  [cm p user]
  (let [trash-path (randomized-trash-path user p)]
    (move cm p trash-path :user user :admin-users (cfg/irods-admins))
    (set-metadata cm trash-path "ipc-trash-origin" p paths/IPCSYSTEM)))

(defn- delete-paths
  [user paths]
  (let [home-matcher #(= (str "/" (cfg/irods-zone) "/home/" user)
                         (ft/rm-last-slash %1))]
    (with-jargon (jargon/jargon-cfg) [cm]
      (let [paths (mapv ft/rm-last-slash paths)]
        (validators/user-exists cm user)
        (validators/all-paths-exist cm paths)
        (validators/user-owns-paths cm user paths)

        ;;; Not allowed to delete the user's home directory.
        (when (some true? (mapv home-matcher paths))
          (throw+ {:error_code ERR_NOT_AUTHORIZED
                   :paths (filterv home-matcher paths)}))

        (doseq [p paths]
          (log/debug "path" p)
          (log/debug "readable?" user (owns? cm user p))

          ;;; Delete all of the tickets associated with the file.
          (let [path-tickets (mapv :ticket-id (ticket-ids-for-path cm (:username cm) p))]
            (doseq [path-ticket path-tickets]
              (delete-ticket cm (:username cm) path-ticket)))

          ;;; If the file isn't already in the user's trash, move it there
          ;;; otherwise, do a hard delete.
          (if-not (.startsWith p (paths/user-trash-path user))
            (move-to-trash cm p user)
            (delete cm p true))) ;;; Force a delete to bypass proxy user's trash.

         {:paths paths}))))

(defn- trash-relative-path
  [path name user-trash]
  (trim-leading-slash
   (ft/path-join
    (or (ft/dirname (string/replace-first path (ft/add-trailing-slash user-trash) ""))
        "")
    name)))


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

(defn- list-in-dir
  [cm fixed-path]
  (let [ffilter (proxy [java.io.FileFilter] [] (accept [stuff] true))]
    (.getListInDirWithFileFilter
      (:fileSystemAO cm)
      (file cm fixed-path)
      ffilter)))

(defn- delete-trash
  [user]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [trash-dir  (paths/user-trash-path user)
          trash-list (mapv #(.getAbsolutePath %) (list-in-dir cm (ft/rm-last-slash trash-dir)))]
      (doseq [trash-path trash-list]
        (delete cm trash-path true))
      {:trash trash-dir
       :paths trash-list})))

(defn do-delete
  [{user :user} {paths :paths}]
  (delete-paths user paths))

(with-pre-hook! #'do-delete
  (fn [params body]
    (paths/log-call "do-delete" params body)
    (validate-map params {:user string?})
    (validate-map body   {:paths sequential?})
    (when (paths/super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED
               :user       (:user params)}))
    (validators/validate-num-paths-under-paths (:user params) (:paths body))))

(with-post-hook! #'do-delete (paths/log-func "do-delete"))

(defn do-delete-contents
  [{user :user} {path :path}]
  (with-jargon (jargon/jargon-cfg) [cm] (validators/path-is-dir cm path))
  (let [paths (directory/get-paths-in-folder user path)]
    (delete-paths user paths)))

(with-pre-hook! #'do-delete-contents
  (fn [params body]
    (paths/log-call "do-delete-contents" params body)
    (validate-map params {:user string?})
    (validate-map body   {:path string?})

    (when (paths/super-user? (:user params))
      (throw+ {:error_code ERR_NOT_AUTHORIZED
               :user       (:user params)}))
    (validators/validate-num-paths-under-folder (:user params) (:path body))))

(with-post-hook! #'do-delete-contents (paths/log-func "do-delete-contents"))

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

(defn do-delete-trash
  [{user :user}]
  (delete-trash user))

(with-post-hook! #'do-delete-trash (paths/log-func "do-delete-trash"))

(with-pre-hook! #'do-delete-trash
  (fn [params]
    (paths/log-call "do-delete-trash" params)
    (validate-map params {:user string?})))

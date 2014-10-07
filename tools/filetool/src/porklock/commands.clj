(ns porklock.commands
  (:use [porklock.pathing]
        [porklock.system]
        [porklock.config]
        [porklock.shell-interop]
        [porklock.fileops :only [absify]]
        [clojure.pprint :only [pprint]])
  (:require [clj-jargon.init :as jg]
            [clj-jargon.item-info :as jg-info]
            [clj-jargon.item-ops :as jg-ops]
            [clj-jargon.metadata :as jg-meta]
            [clj-jargon.permissions :as jg-perms]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure-commons.file-utils :as ft]))

(defn init-jargon
  [cfg-path]
  (load-config-from-file cfg-path)
  (jg/init (irods-host)
           (irods-port)
           (irods-user)
           (irods-pass)
           (irods-home)
           (irods-zone)
           (irods-resc)))

(defn fix-meta
  [m]
  (cond
    (= (count m) 3) m
    (= (count m) 2) (conj m "default-unit")
    (= (count m) 1) (concat m ["default-value" "default-unit"])
    :else           []))

(defn avu?
  [cm path attr value]
  (filter #(= value (:value %)) (jg-meta/get-attribute cm path attr)))

(def porkprint (partial println "[porklock] "))

(defn apply-metadata
  [cm destination meta]
  (let [tuples (map fix-meta meta)
        dest   (ft/rm-last-slash destination)]
    (porkprint "Metadata tuples for " destination " are  " tuples)
    (when (pos? (count tuples))
      (doseq [tuple tuples]
        (porkprint "Size of tuple " tuple " is " (count tuple))
        (when (= (count tuple) 3)
          (porkprint "Might be adding metadata to " dest " " tuple)
          (porkprint "AVU? " dest (avu? cm dest (first tuple) (second tuple)))
          (when (empty? (avu? cm dest (first tuple) (second tuple)))
            (porkprint "Adding metadata " (first tuple) " " (second tuple) " " dest)
            (apply (partial jg-meta/add-metadata cm dest) tuple)))))))

(defn irods-env-contents
  [options]
  (str
    "irodsHost "     (irods-host) "\n"
    "irodsPort "     (irods-port) "\n"
    "irodsUserName " (irods-user) "\n"
    "irodsZone "     (irods-zone) "\n"
    "irodsHome "     (irods-home) "\n"))

(defn make-irods-env
  [env]
  (shell-out [(iinit-path) :in (irods-pass) :env env] :skip-err true))

(defn icommands-env
  "Constructs an environment variable map for the icommands."
  [options]
  (let [env {"irodsAuthFileName" (irods-auth-filepath)
             "irodsEnvFile"      (irods-env-filepath)}]
    (spit (irods-env-filepath) (irods-env-contents options))
    (make-irods-env env)
    (merge env {"clientUserName" (:user options)})))

(defn user-home-dir
  [cm username]
  (ft/path-join "/" (:zone cm) "home" username))

(defn home-folder?
  [zone full-path]
  (let [parent (ft/dirname full-path)]
    (= parent (ft/path-join "/" zone "home"))))

(defn halting-folders
  [cm username]
  (set (user-home-dir cm username)
    (ft/path-join "/" (:zone cm) "home" "shared")))

(defn halt?
  [cm username path-to-test]
  (or (contains? (halting-folders cm username) path-to-test)
      (home-folder? (:zone cm) path-to-test)))

(defn set-parent-owner
  [cm username dir-dest]
  (loop [p (ft/dirname dir-dest)]
    (when-not (halt? cm username p)
      (if-not (jg-perms/owns? cm username p )
        (jg-perms/set-owner cm p username))
      (recur (ft/dirname p)))))

(defn iput-command
  "Runs the iput icommand, tranferring files from the --source
   to the remote --destination."
  [options]
  (let [source-dir      (ft/abs-path (:source options))
        dest-dir        (:destination options)
        irods-cfg       (init-jargon (:config options))
        ic-env          (icommands-env options)
        transfer-files  (files-to-transfer options)
        metadata        (:meta options)
        skip-parent?    (:skip-parent-meta options)
        dest-files      (relative-dest-paths transfer-files source-dir dest-dir)]
    (jg/with-jargon irods-cfg [cm]
      (when-not (jg-info/exists? cm (ft/dirname dest-dir))
        (porkprint (ft/dirname dest-dir) "does not exist.")
        (System/exit 1))

      (when (and (not (jg-perms/is-writeable? cm (:user options) (ft/dirname dest-dir)))
                 (not= (user-home-dir cm (:user options))
                       (ft/rm-last-slash dest-dir)))
        (porkprint (ft/dirname dest-dir) "is not writeable.")
        (System/exit 1))

      (when-not (jg-info/exists? cm dest-dir)
        (porkprint "Path " dest-dir " does not exist. Creating it.")
        (jg-ops/mkdirs cm dest-dir))

      (when-not (jg-perms/owns? cm (:user options) dest-dir)
        (porkprint "Setting the owner of " dest-dir " to " (:user options))
        (jg-perms/set-owner cm dest-dir (:user options)))

      (doseq [[src dest]  (seq dest-files)]
        (let [dir-dest (ft/dirname dest)]

          ;;; It's possible that the destination directory doesn't
          ;;; exist yet in iRODS, so create it if it's not there.
          (porkprint "Creating all directories in iRODS down to " dir-dest)
          (when-not (jg-info/exists? cm dir-dest)
            (jg-ops/mkdirs cm dir-dest))

          ;;; The destination directory needs to be tagged with AVUs
          ;;; for the App and Execution.
          (porkprint "Applying metadata to" dir-dest)
          (apply-metadata cm dir-dest metadata)

          ;;; Since we run as a proxy account, the destination directory
          ;;; needs to have the owner set to the user that ran the app.
          (when-not (jg-perms/owns? cm (:user options) dir-dest)
            (porkprint "Setting owner of " dir-dest " to " (:user options))
            (jg-perms/set-owner cm dir-dest (:user options)))

          (shell-out [(iput-path) "-f" "-P" src dest :env ic-env])

          ;;; Apply the App and Execution metadata to the newly uploaded
          ;;; file/directory.
          (porkprint "Applying metadata to " dest)
          (apply-metadata cm dest metadata)))

      (when-not skip-parent?
        (porkprint "Applying metadata to " dest-dir)
        (apply-metadata cm dest-dir metadata)
        (doseq [fileobj (file-seq (jg-info/file cm dest-dir))]
          (let [filepath (.getAbsolutePath fileobj)
                dir?     (.isDirectory fileobj)]
            (jg-perms/set-owner cm filepath (:user options))
            (apply-metadata cm filepath metadata)))))))

(defn- iget-args
  [source destination env]
  (filter #(not (nil? %))
          [(iget-path)
           "-f"
           "-P"
           (if (.endsWith source "/")
             "-r")
           (ft/rm-last-slash source)
           (ft/add-trailing-slash destination)
           :env env]))

(defn apply-input-metadata
  [cm user fpath meta]
  (if-not (jg-info/is-dir? cm fpath)
    (if (jg-perms/owns? cm user fpath)
      (apply-metadata cm fpath meta))
    (doseq [f (file-seq (jg-info/file cm fpath))]
      (let [abs-path (.getAbsolutePath f)]
        (if (jg-perms/owns? cm user abs-path)
          (apply-metadata cm abs-path meta))))))

(defn iget-command
  "Runs the iget icommand, retrieving files from --source
   to the local --destination."
  [options]
  (let [source    (:source options)
        dest      (:destination options)
        irods-cfg (init-jargon (:config options))
        ic-env    (icommands-env options)
        srcdir    (ft/rm-last-slash source)
        args      (iget-args source dest ic-env)
        metadata  (:meta options)]
    (jg/with-jargon irods-cfg [cm]
      (apply-input-metadata cm (:user options) srcdir metadata)
      (shell-out args))))

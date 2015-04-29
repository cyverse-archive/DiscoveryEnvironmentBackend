(ns porklock.commands
  (:use [porklock.pathing]
        [porklock.system]
        [porklock.config]
        [porklock.fileops :only [absify]]
        [clojure.pprint :only [pprint]]
        [slingshot.slingshot :only [try+]])
  (:require [clj-jargon.init :as jg]
            [clj-jargon.item-info :as info]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.metadata :as meta]
            [clj-jargon.permissions :as perms]
            [clojure.java.io :as io]
            [clojure-commons.file-utils :as ft]))

(def porkprint (partial println "[porklock] "))

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

(defn retry
  "Attempt calling (func) with args a maximum of 'times' times if an error occurs.
   Adapted from a stackoverflow solution: http://stackoverflow.com/a/12068946"
  [max-attempts func & args]
  (let [result (try
                  {:value (apply func args)}
                  (catch Exception e
                    (porkprint "Error calling a function. " max-attempts " attempts remaining: " e)
                    (if-not (pos? max-attempts)
                      (throw e)
                      {:exception e})))]
    (if (:exception result)
      (recur (dec max-attempts) func args)
      (:value result))))

(defn fix-meta
  [m]
  (cond
    (= (count m) 3) m
    (= (count m) 2) (conj m "default-unit")
    (= (count m) 1) (concat m ["default-value" "default-unit"])
    :else           []))

(defn avu?
  [cm path attr value]
  (filter #(= value (:value %)) (meta/get-attribute cm path attr)))

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
            (apply (partial meta/add-metadata cm dest) tuple)))))))

(defn user-home-dir
  [cm username]
  (ft/path-join "/" (:zone cm) "home" username))

(defn home-folder?
  [zone full-path]
  (let [parent (ft/dirname full-path)]
    (= parent (ft/path-join "/" zone "home"))))


(defn iput-status
  "Callback function for the overallStatus function for a TransferCallbackListener."
  [transfer-status]
  (let [exc (.getTransferException transfer-status)]
    (if-not (nil? exc)
      (throw (Exception. (str exc)))))
  nil)

(defn iput-status-cb
  "Callback function for the statusCallback function of a TransferCallbackListener."
  [transfer-status]
  (porkprint "-------")
  (porkprint "iput status update:")
  (porkprint "\ttransfer state: " (.getTransferState transfer-status))
  (porkprint "\ttransfer type: " (.getTransferType transfer-status))
  (porkprint "\tsource path: " (.getSourceFileAbsolutePath transfer-status))
  (porkprint "\tdest path: " (.getTargetFileAbsolutePath transfer-status))
  (porkprint "\tfile size: " (.getTotalSize transfer-status))
  (porkprint "\tbytes transferred: " (.getBytesTransfered transfer-status))
  (porkprint "\tfiles to transfer: " (.getTotalFilesToTransfer transfer-status))
  (porkprint "\tfiles skipped: " (.getTotalFilesSkippedSoFar transfer-status))
  (porkprint "\tfiles transferred: " (.getTotalFilesTransferredSoFar transfer-status))
  (porkprint "\ttransfer host: " (.getTransferHost transfer-status))
  (porkprint "\ttransfer zone: " (.getTransferZone transfer-status))
  (porkprint "\ttransfer resource: " (.getTargetResource transfer-status))
  (porkprint "-------")
  (let [exc (.getTransferException transfer-status)]
    (if-not (nil? exc)
      (do (porkprint "got an exception in iput: " exc)
        ops/skip
      ops/continue))))

(defn iput-force-cb
  "Callback function for the transferAsksWhetherToForceOperation function of a
   TransferCallbackListener."
   [abs-path collection?]
   (porkprint "force iput of " abs-path ". collection?: " collection?)
   ops/yes-for-all)

(def tcl (ops/transfer-callback-listener iput-status iput-status-cb iput-force-cb))

(defn iput-command
  "Runs the iput icommand, tranferring files from the --source
   to the remote --destination."
  [options]
  (let [source-dir     (ft/abs-path (:source options))
        dest-dir       (:destination options)
        irods-cfg      (init-jargon (:config options))
        transfer-files (files-to-transfer options)
        metadata       (:meta options)
        skip-parent?   (:skip-parent-meta options)
        dest-files     (relative-dest-paths transfer-files source-dir dest-dir)
        error?         (atom false)
        user           (:user options)]
    (jg/with-jargon irods-cfg :client-user user [cm]
      (when-not (info/exists? cm (ft/dirname dest-dir))
        (porkprint (ft/dirname dest-dir) "does not exist.")
        (System/exit 1))
      (when (and (not (perms/is-writeable? cm user (ft/dirname dest-dir)))
                 (not= (user-home-dir cm user) (ft/rm-last-slash dest-dir)))
        (porkprint (ft/dirname dest-dir) "is not writeable.")
        (System/exit 1))
      (when-not (info/exists? cm dest-dir)
        (porkprint "Path " dest-dir " does not exist. Creating it.")
        (ops/mkdir cm dest-dir))
      (doseq [[src dest] (seq dest-files)]
        (let [dir-dest (ft/dirname dest)]
          (if-not (or (.isFile (io/file src))
                      (.isDirectory (io/file src)))
            (porkprint "Path " src " is neither a file nor a directory.")
            (do
              ;;; It's possible that the destination directory doesn't
              ;;; exist yet in iRODS, so create it if it's not there.
              (porkprint "Creating all directories in iRODS down to " dir-dest)
              (when-not (info/exists? cm dir-dest)
                (ops/mkdirs cm dir-dest))

              ;;; The destination directory needs to be tagged with AVUs
              ;;; for the App and Execution.
              (porkprint "Applying metadata to" dir-dest)
              (apply-metadata cm dir-dest metadata)

              (try
                (retry 10 ops/iput cm src dest tcl)
                (catch Exception err
                  (porkprint "iput failed: " err)
                  (reset! error? true)))

              ;;; Apply the App and Execution metadata to the newly uploaded
              ;;; file/directory.
              (porkprint "Applying metadata to " dest)
              (apply-metadata cm dest metadata)))))

      (when-not skip-parent?
        (porkprint "Applying metadata to " dest-dir)
        (apply-metadata cm dest-dir metadata)
        (doseq [fileobj (file-seq (info/file cm dest-dir))]
          (apply-metadata cm (.getAbsolutePath fileobj) metadata)))

      ;;; Transfer files from the NFS mount point into the logs
      ;;; directory of the destination
      (if (and (System/getenv "SCRIPT_LOCATION") (not skip-parent?))
        (let [script-loc  (ft/dirname (ft/abs-path (System/getenv "SCRIPT_LOCATION")))
              dest        (ft/path-join dest-dir "logs")
              exclude-map (merge options {:source script-loc})
              exclusions  (set (exclude-files-from-dir exclude-map))]
          (porkprint "Exclusions:\n" exclusions)
          (doseq [fileobj (file-seq (clojure.java.io/file script-loc))]
            (let [src (.getAbsolutePath fileobj)
                  dest-path (ft/path-join dest (ft/basename src))]
              (try+
               (when-not (or (.isDirectory fileobj) (contains? exclusions src))
                 (retry 10 ops/iput cm src dest tcl)
                 (apply-metadata cm dest-path metadata))
               (catch [:error_code "ERR_BAD_EXIT_CODE"] err
                 (porkprint "Command exited with a non-zero status: " err)
                 (reset! error? true)))))))

      (if @error?
        (throw (Exception. "An error occurred tranferring files into iRODS. Please check the above logs for more information."))))))

(defn apply-input-metadata
  [cm user fpath meta]
  (if-not (info/is-dir? cm fpath)
    (if (perms/owns? cm user fpath)
      (apply-metadata cm fpath meta))
    (doseq [f (file-seq (info/file cm fpath))]
      (let [abs-path (.getAbsolutePath f)]
        (if (perms/owns? cm user abs-path)
          (apply-metadata cm abs-path meta))))))

(defn iget-command
  "Runs the iget icommand, retrieving files from --source
   to the local --destination."
  [options]
  (let [source    (:source options)
        dest      (:destination options)
        irods-cfg (init-jargon (:config options))
        srcdir    (ft/rm-last-slash source)
        metadata  (:meta options)]
    (jg/with-jargon irods-cfg :client-user (:user options) [cm]
      (apply-input-metadata cm (:user options) srcdir metadata)
      (retry 10 ops/iget cm source dest tcl))))

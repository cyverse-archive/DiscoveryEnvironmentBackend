(ns clj-jargon.item-ops
  (:use [clj-jargon.validations]
        [clj-jargon.permissions])
  (:require [clojure-commons.file-utils :as ft]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [throw+]]
            [clojure-commons.error-codes :refer [ERR_NOT_A_FOLDER]]
            [clj-jargon.item-info :as info])
  (:import [org.irods.jargon.core.packinstr DataObjInp$OpenFlags]
           [org.irods.jargon.core.pub DataTransferOperations IRODSFileSystemAO]  ; needed for cursive type navigation
           [org.irods.jargon.core.pub.io IRODSFileReader]
           [org.irods.jargon.core.transfer TransferStatusCallbackListener
              TransferStatusCallbackListener$FileStatusCallbackResponse
              TransferStatusCallbackListener$CallbackResponse
              DefaultTransferControlBlock]))

(defn mkdir
  [cm dir-path]
  (validate-full-dirpath dir-path)
  (validate-path-lengths dir-path)
  (.mkdir (:fileSystemAO cm) (info/file cm dir-path) false))


;; bad Exit conditions:  parent Is file, or reaches zone level
(defn- mkdirs-unvalidated
  [cm dir-path]
  (log/trace "mkdirs-unvalidated dir-path =" dir-path)
  (let [parent (ft/rm-last-slash (ft/dirname dir-path))]
    (if (info/exists? cm parent)
      (when (or (info/is-file? cm parent)
                (= "/" (ft/dirname parent)))
        (throw+ {:error_code ERR_NOT_A_FOLDER :path parent}))
      (mkdirs-unvalidated cm parent))
    (.mkdir (:fileSystemAO cm) (info/file cm dir-path) false)))


(defn mkdirs
  [cm dir-path]
  (validate-full-dirpath dir-path)
  (validate-path-lengths dir-path)

  ; iRODS always returns success even when it fails to create a directory recursively.
  #_(.mkdir (:fileSystemAO cm) (file cm dir-path) true)

  ; Here is a work around until this bug is fixed in iRODS.
  (mkdirs-unvalidated cm dir-path))


(defn delete
  ([cm a-path]
   (delete cm a-path false))
  ([cm a-path force?]
   (validate-path-lengths a-path)
   (let [fileSystemAO (:fileSystemAO cm)
         resource     (info/file cm a-path)]
     (if (and (:use-trash cm) (false? force?))
       (if (info/is-dir? cm a-path)
         (.directoryDeleteNoForce fileSystemAO resource)
         (.fileDeleteNoForce fileSystemAO resource))
       (if (info/is-dir? cm a-path)
         (.directoryDeleteForce fileSystemAO resource)
         (.fileDeleteForce fileSystemAO resource))))))

(defn move
  "Moves a file/dir from source path 'source' into destination directory 'dest'.

   Parameters:
     source - String containing the path to the file/dir being moved.
     dest - String containing the path to the destination directory. Should not end with a slash.
     :admin-users (optional) - List of users that must retain ownership on the file/dir being moved.
     :user (optional) - The username of the user performing the move.
     :skip-source-perms? (optional) - Boolean the tells move to skip ensuring that permissions for
                                      the admin users are correct."
  [cm source dest & {:keys [admin-users user skip-source-perms?]
                     :or   {admin-users #{}
                            skip-source-perms? false}}]
  (validate-path-lengths source)
  (validate-path-lengths dest)
  (let [fileSystemAO (:fileSystemAO cm)
        src          (info/file cm source)
        dst          (info/file cm dest)]
    (if (info/is-file? cm source)
      (.renameFile fileSystemAO src dst)
      (.renameDirectory fileSystemAO src dst))
    (fix-perms cm src dst user admin-users skip-source-perms?)))

(defn move-all
  [cm sources dest & {:keys [admin-users user]
                      :or {admin-users #{}}}]
  (doseq [s sources] (validate-path-lengths (ft/path-join dest (ft/basename s))))
  (dorun
   (map
    #(move cm %1 (ft/path-join dest (ft/basename %1)) :user user :admin-users admin-users)
    sources)))


(defn- output-stream
  "Returns an FileOutputStream for a file in iRODS pointed to by 'output-path'. If the file exists,
   it will be truncated."
  [cm output-path]
  (.instanceIRODSFileOutputStream (:fileFactory cm)
                                  (info/file cm output-path)
                                  DataObjInp$OpenFlags/WRITE_TRUNCATE))


(defn input-stream
  "Returns a FileInputStream for a file in iRODS pointed to by 'input-path'"
  [cm input-path]
  (validate-path-lengths input-path)
  (.instanceIRODSFileInputStream (:fileFactory cm) (info/file cm input-path)))


(defn read-file
  [cm fpath buffer]
  (validate-path-lengths fpath)
  (.read (IRODSFileReader. (info/file cm fpath) (:fileFactory cm)) buffer))


(defn data-transfer-obj
  [cm]
  (.getDataTransferOperations (:accessObjectFactory cm) (:irodsAccount cm)))

(defn copy
  [cm source dest]
  (validate-path-lengths source)
  (validate-path-lengths dest)

  (let [dto (data-transfer-obj cm)
        res (or (:defaultResource cm) "demoResc")]
    (.copy dto source res dest nil nil)))

(defn copy-stream
  [cm istream user dest-path]
  (validate-path-lengths dest-path)
  (let [ostream (output-stream cm dest-path)]
    (try
      (io/copy istream ostream)
      (finally
        (.close istream)
        (.close ostream)
        (set-owner cm dest-path user)))
    (info/stat cm dest-path)))


(def continue TransferStatusCallbackListener$FileStatusCallbackResponse/CONTINUE)
(def skip TransferStatusCallbackListener$FileStatusCallbackResponse/SKIP)
(def cancel TransferStatusCallbackListener$CallbackResponse/CANCEL)
(def no-for-all TransferStatusCallbackListener$CallbackResponse/NO_FOR_ALL)
(def no-this-file TransferStatusCallbackListener$CallbackResponse/NO_THIS_FILE)
(def yes-for-all TransferStatusCallbackListener$CallbackResponse/YES_FOR_ALL)
(def yes-this-file TransferStatusCallbackListener$CallbackResponse/YES_THIS_FILE)
(def tcb (DefaultTransferControlBlock/instance))

(defn transfer-callback-listener
  "Returns an instance of TransferStatusCallbackListener with the overallStatusCallback(),
   statusCallback(), and transferAsksWhetherToForceOperation() functions delegated to the
   functions passed in."
  [overall-status-callback-fn status-callback-fn transfer-asks-fn]
  (reify TransferStatusCallbackListener
    (overallStatusCallback [_ transfer-status]
      (overall-status-callback-fn transfer-status))
    (statusCallback [_ transfer-status]
      (status-callback-fn transfer-status))
    (transferAsksWhetherToForceOperation [_ abs-path collection?]
      (transfer-asks-fn abs-path collection?))))

(defn iput
  "Transfers local-path to remote-path, using tcl as the TransferStatusCallbackListener.
   tcl can also be set to nil."
  [cm local-path remote-path tcl]
  (let [dto (data-transfer-obj cm)]
    (.putOperation dto local-path remote-path "" tcl nil)))

(defn iget
  "Transfers remote-path to local-path, using tcl as the TransferStatusCallbackListener"
  ([cm remote-path local-path tcl]
    (iget cm remote-path local-path tcl tcb))
  ([cm remote-path local-path tcl control-block]
    (let [dto (data-transfer-obj cm)]
      (.getOperation dto remote-path local-path "" tcl control-block))))

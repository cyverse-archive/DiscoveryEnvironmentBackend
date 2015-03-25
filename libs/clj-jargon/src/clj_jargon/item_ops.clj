(ns clj-jargon.item-ops
  (:use [clj-jargon.validations]
        [clj-jargon.item-info]
        [clj-jargon.permissions])
  (:require [clojure-commons.file-utils :as ft]
            [clojure.java.io :as io])
  (:import [org.irods.jargon.core.pub.io IRODSFileReader]))

(defn mkdir
  [cm dir-path]
  (validate-full-dirpath dir-path)
  (validate-path-lengths dir-path)
  (.mkdir (:fileSystemAO cm) (file cm dir-path) true))

(defn mkdirs
  [cm dir-path]
  (validate-full-dirpath dir-path)
  (validate-path-lengths dir-path)
  (.mkdirs (file cm dir-path)))

(defn delete
  ([cm a-path]
   (delete cm a-path false))
  ([cm a-path force?]
   (validate-path-lengths a-path)
   (let [fileSystemAO (:fileSystemAO cm)
         resource     (file cm a-path)]
     (if (and (:use-trash cm) (false? force?))
       (if (is-dir? cm a-path)
         (.directoryDeleteNoForce fileSystemAO resource)
         (.fileDeleteNoForce fileSystemAO resource))
       (if (is-dir? cm a-path)
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
        src          (file cm source)
        dst          (file cm dest)]

    (if (is-file? cm source)
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

(defn output-stream
  "Returns an FileOutputStream for a file in iRODS pointed to by 'output-path'."
  [cm output-path]
  (validate-path-lengths output-path)
  (.instanceIRODSFileOutputStream (:fileFactory cm) (file cm output-path)))

(defn input-stream
  "Returns a FileInputStream for a file in iRODS pointed to by 'input-path'"
  [cm input-path]
  (validate-path-lengths input-path)
  (.instanceIRODSFileInputStream (:fileFactory cm) (file cm input-path)))

(defn read-file
  [cm fpath buffer]
  (validate-path-lengths fpath)
  (.read (IRODSFileReader. (file cm fpath) (:fileFactory cm)) buffer))

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
  (let [ostream (output-stream cm dest-path)]
    (try
      (io/copy istream ostream)
      (finally
        (.close istream)
        (.close ostream)
        (set-owner cm dest-path user)))
    (stat cm dest-path)))

(ns porklock.validation
  (:use [porklock.pathing]
        [clojure.pprint]
        [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes])
  (:require [clojure-commons.file-utils :as ft]))

(def ERR_MISSING_OPTION "ERR_MISSING_OPTION")
(def ERR_PATH_NOT_ABSOLUTE "ERR_PATH_NOT_ABSOLUTE")
(def ERR_ACCESS_DENIED "ERR_ACCESS_DENIED")

(defn usable?
  [user]
  true)

(defn validate-put
  "Validates information for a put operation.
   Throws an error if the input is invalid.

   For a put op, all of the local files must exist,
   all of the --include files must exist, all
   of the .irods/* files must exist, and the paths
   to the executable must exist."
  [options]
  (if-not (:user options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--user"}))

  (if-not (usable? (:user options))
    (throw+ {:error_code ERR_ACCESS_DENIED}))

  (if-not (:source options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--source"}))

  (if-not (:destination options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--destination"}))

  (if-not (ft/dir? (:source options))
      (throw+ {:error_code ERR_NOT_A_FOLDER
               :path (:source options)}))

  (if-not (ft/abs-path? (:destination options))
    (throw+ {:error_code ERR_PATH_NOT_ABSOLUTE
             :path (:destination options)}))

  (if-not (:config options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--config"}))

  (println "Files to upload: ")
    (pprint (files-to-transfer options))
    (println " ")

  (let [paths-to-check (flatten [(files-to-transfer options)
                                 (:config options)])]

    (println "Paths to check: ")
    (pprint paths-to-check)
    (doseq [p paths-to-check]
      (if (not (ft/exists? p))
        (throw+ {:error_code ERR_DOES_NOT_EXIST
                 :path p})))))

(defn validate-get
  "Validates info for a get op. Throws an error
   on invalid input.

   For a get op, the following files must exist.
     * Path to 'iget'.
     * Destination directory.
     * .irods/.irodsA and .irods/.irodsEnv files.
   Additionally:
     * Destination must be a directory."
  [options]
  (if-not (:user options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--user"}))

  (if-not (usable? (:user options))
    (throw+ {:error_code ERR_ACCESS_DENIED}))

  (if-not (:source options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--source"}))

  (if-not (:destination options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--destination"}))

  (if-not (:config options)
    (throw+ {:error_code ERR_MISSING_OPTION
             :option "--config"}))

  (let [paths-to-check (flatten [(:destination options)
                                 (:config options)])]
    (doseq [p paths-to-check]
      (if (not (ft/exists? p))
        (throw+ {:error_code ERR_DOES_NOT_EXIST
                 :path p})))

    (if-not (ft/dir? (:destination options))
      (throw+ {:error_code ERR_NOT_A_FOLDER
               :path (:destination options)}))))

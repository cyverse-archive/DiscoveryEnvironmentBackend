(ns porklock.system
  (:use [porklock.fileops :only (filenames-in-dir)])
  (:require [clojure-commons.file-utils :as ft]
            [clojure.string :as string]))

(defn system-env
  "Returns values for the specified environment variable
   If no parameter is specified, then a map of all of
   environment variables is returned."
  ([]
     (System/getenv))
  ([var-name]
     (System/getenv var-name)))

(defn dirs-in-path
  "Returns a sequence of directory paths in the PATH environment variable."
  []
  (filter 
    ft/dir? 
    (string/split (system-env "PATH") (re-pattern java.io.File/pathSeparator))))

(defn find-file-in-path
  "Searchs the $PATH for a file. Returns the full path to the file."
  [filename]
  (loop [dirs (dirs-in-path)]
    (cond
      (= (count dirs) 0) 
      ""
      
      (contains? (set (filenames-in-dir (first dirs))) filename)
      (ft/path-join (first dirs) filename)
      
      :else
      (recur (rest dirs)))))
(ns porklock.pathing
  (:use [clojure.set]
        [porklock.fileops])
  (:require [clojure.string :as string]
            [clojure-commons.file-utils :as ft]))

(defn relative-paths-to-exclude
  []
  [".irods"
   ".irods/.irodsA"
   ".irods/.irodsEnv"
   "logs/irods-config"
   "irods.retries"
   "irods.lfretries"
   "irods-config"
   ".irodsA"
   ".irodsEnv"])

(defn exclude-files-from-dir
  "Splits up the exclude option and turns the result into paths in the source dir."
  [{source :source excludes :exclude delimiter :exclude-delimiter}]
  (let [irods-files (relative-paths-to-exclude)]
    (if-not (string/blank? excludes)
      (mapv
        #(if-not (.startsWith % "/")
           (ft/path-join source %)
           %)
        (concat
          (string/split excludes (re-pattern delimiter))
          irods-files))
      (mapv
        #(ft/path-join source %)
        irods-files))))

(defn exclude-files
  "Splits up the exclude option and turns them all into absolute paths."
  [{excludes :exclude delimiter :exclude-delimiter :as in-map}]
  (let [irods-files (relative-paths-to-exclude)]
    (if-not (string/blank? excludes)
      (concat
        (exclude-files-from-dir in-map)
        (absify (concat (string/split excludes (re-pattern delimiter)) irods-files)))
      (concat (exclude-files-from-dir in-map) irods-files))))

(defn include-files
  "Splits up the include option and turns them all into absolute paths."
  [{includes :include delimiter :include-delimiter}]
  (if-not (string/blank? includes)
    (absify (string/split includes (re-pattern delimiter)))
    []))

(defn path-matches?
  "Determines whether or not a path matches a filter path.  If the filter path
   refers to a directory then all descendents of the directory match.
   Otherwise, only that exact path matches."
  [path filter-path]
  (if (ft/dir? filter-path)
    (.startsWith path filter-path)
    (= path filter-path)))

(defn should-not-exclude?
  "Determines whether or not a file should be excluded based on the list of
   excluded files."
  [excludes path]
  (not-any? #(path-matches? path %) excludes))

(defn filtered-files
  "Constructs a list of files that shouldn't be filtered out by the list of
   excluded files."
  [source-dir excludes]
  (filter #(should-not-exclude? excludes %) (files-and-dirs source-dir)))

(defn files-to-transfer
  "Constructs a list of the files that need to be transferred."
  [options]
  (let [includes (set (include-files options))
        excludes (exclude-files options)
        allfiles (set (filtered-files (:source options) (exclude-files options)))]
    (println "EXCLUDING: " excludes)
    (vec (union allfiles includes))))

(defn- str-contains?
  [s match]
  (if (not= (.indexOf s match) -1)
    true
    false))

(defn- fix-path
  [transfer-file sdir ddir]
  (ft/rm-last-slash (ft/path-join ddir (string/replace transfer-file (re-pattern sdir) ""))))

(defn relative-dest-paths
  "Constructs a list of absolute destination paths based on the
   input and the given source directory."
  [transfer-files source-dir dest-dir]

  (let [sdir (ft/add-trailing-slash source-dir)]
    (apply
      merge
      (map
        #(if (str-contains? %1 sdir)
           {%1 (fix-path %1 sdir dest-dir)}
           {%1 %1})
        transfer-files))))

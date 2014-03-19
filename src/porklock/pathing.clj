(ns porklock.pathing
  (:use [clojure.set]
        [clojure.java.io]
        [porklock.fileops]
        [porklock.system])
  (:require [clojure.string :as string]
            [clojure-commons.file-utils :as ft]))

(defn pwd
  "Returns the path to the current working directory."
  []
  (System/getProperty "user.dir"))

(defn user-irods-dir
  "Returns the full path to the user's .irods directory."
  []
  (.mkdirs (as-file "./.irods"))
  "./.irods")

(defn irods-auth-filepath
  "Returns the path where the .irodsA should be."
  []
  (ft/path-join (user-irods-dir) ".irodsA"))

(defn irods-env-filepath
  "Returns the path to where the .irodsEnv file should be."
  []
  (ft/path-join (user-irods-dir) ".irodsEnv"))

(defn imkdir-path
  "Returns the path to the imkdir executable or an empty
   string if imkdir couldn't be found."
  []
  (find-file-in-path "imkdir"))

(defn iput-path
  "Returns the path to the iput executable or an empty
   string if iput couldn't be found."
  []
  (find-file-in-path "iput"))

(defn iget-path
  "Returns the path to the iget executable, or an empty
   string if iget cannot be found."
  []
  (find-file-in-path "iget"))

(defn ils-path
  "Returns the path to the ils executable or an empty
   string if ils couldn't be found."
  []
  (find-file-in-path "ils"))

(defn iinit-path
  "Returns the path to the iinit executable or an empty
   string if iinit couldn't be found."
  []
  (find-file-in-path "iinit"))


(defn relative-paths-to-exclude
  []
  [(irods-auth-filepath) 
   (irods-env-filepath) 
   ".irods" 
   ".irods/.irodsA" 
   ".irods/.irodsEnv"
   "logs/irods-config"])

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


(ns clavin.properties
  (:use [clojure-commons.props])
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]))

(defn prop-files
  [fpath]
  (if (ft/file? fpath)
    (if (re-find #"\.properties$" fpath) [fpath] [])
    (apply vector
           (filter
             #(re-find #"\.properties$" %)
             (map #(.getPath %) (file-seq (io/file fpath)))))))

(defn parse-files
  [fpath]
  (apply merge (for [pfile (prop-files fpath)]
                 {(.replaceAll (re-matcher #"\.properties$" (ft/basename pfile)) "")
                  (read-properties pfile)})))

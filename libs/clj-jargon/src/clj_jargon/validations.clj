(ns clj-jargon.validations
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes])
  (:require [clojure-commons.file-utils :as ft]))


(def max-path-length 1067)
(def max-dir-length 640)
(def max-filename-length (- max-path-length max-dir-length))
(def ERR_BAD_DIRNAME_LENGTH "ERR_BAD_DIRNAME_LENGTH")
(def ERR_BAD_BASENAME_LENGTH "ERR_BAD_BASENAME_LENGTH")
(def ERR_BAD_PATH_LENGTH "ERR_BAD_PATH_LENGTH")


(defn validate-full-dirpath
  [full-dirpath]
  (if (> (count full-dirpath) max-dir-length)
    (throw+ {:error_code ERR_BAD_DIRNAME_LENGTH
             :dir-path full-dirpath
             :full-path full-dirpath})))

(defn validate-path-lengths
  [full-path]
  (let [dir-path (ft/dirname full-path)
        file-path (ft/basename full-path)]
    (cond
     (> (count full-path) max-path-length)
     (throw+ {:error_code ERR_BAD_PATH_LENGTH
              :full-path full-path})

     (> (count dir-path) max-dir-length)
     (throw+ {:error_code ERR_BAD_DIRNAME_LENGTH
              :dir-path dir-path
              :full-path full-path})

     (> (count file-path) max-filename-length)
     (throw+ {:error_code ERR_BAD_BASENAME_LENGTH
              :file-path file-path
              :full-path full-path}))))

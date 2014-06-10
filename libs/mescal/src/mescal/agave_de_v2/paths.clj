(ns mescal.agave-de-v2.paths
  (:use [clojure.java.io :only [file]])
  (:require [clojure.string :as string]))

(defn- remove-trailing-slash
  [path]
  (string/replace path #"/$" ""))

(defn- remove-leading-slash
  [path]
  (string/replace path #"^/" ""))

(defn agave-path
  [irods-home path]
  (when-not (nil? path)
    (string/replace path (re-pattern (str "^\\Q" (remove-trailing-slash irods-home))) "")))

(defn de-path
  [irods-home path]
  (when-not (nil? path)
    (str (file irods-home (remove-leading-slash path)))))

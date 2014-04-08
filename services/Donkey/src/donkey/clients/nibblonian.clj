(ns donkey.clients.nibblonian
  (:use [donkey.util.config]
        [donkey.util.service :only [build-url-with-query]]
        [donkey.util.transformers :only [add-current-user-to-map]]
        [donkey.auth.user-attributes :only [current-user]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [donkey.services.filesystem.common-paths :as cp]
            [donkey.services.filesystem.create :as cr]
            [donkey.services.filesystem.stat :as st]
            [donkey.services.filesystem.exists :as e]))

(defn home-dir
  "Determines the home folder for the current user."
  []
  (cp/user-home-dir (:shortUsername current-user)))

(defn create
  "Creates a directory."
  [path]
  (cr/create (:shortUsername current-user) path))

(defn exists?
  "Determines whether or not a path exists."
  [path]
  (e/path-exists? path))

(defn stat
  "Obtains file status information for a path."
  [path]
  (st/path-stat (:shortUsername current-user) path))

(defn get-or-create-dir
  "Returns the path argument if the path exists and refers to a directory.  If
   the path exists and refers to a regular file then nil is returned.
   Otherwise, a new directory is created and the path is returned."
  [path]
  (log/debug "getting or creating dir: path =" path)
  (cond
   (not (e/path-exists? path))
   (create path)

   (and (e/path-exists? path) (st/path-is-dir? path))
   path

   (and (e/path-exists? path) (not (st/path-is-dir? path)))
   nil

   :else
   nil))

(defn gen-output-dir
  "Either obtains or creates a default output directory using a specified base name."
  [base]
  (first
   (remove #(nil? (get-or-create-dir %))
           (cons base (map #(str base "-" %) (iterate inc 1))))))

(defn build-path
  "Builds a path from a base path and a list of components."
  [path & components]
  (string/join
   "/"
   (cons (string/replace path #"/+$" "")
         (map #(string/replace % #"^/+|/+$" "") components))))

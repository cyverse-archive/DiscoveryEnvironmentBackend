(ns donkey.services.filesystem.exists
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.util.config]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [exists?]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [cemerick.url :as url]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.permissions :as fs-perm]
            [donkey.services.filesystem.validators :as validators]
            [donkey.services.filesystem.uuids :as uuid]))

(defn- url-encoded?
  [string-to-check]
  (re-seq #"\%[A-Fa-f0-9]{2}" string-to-check))

(defn- url-decode
  [string-to-decode]
  (if (url-encoded? string-to-decode)
    (url/url-decode string-to-decode)
    string-to-decode))

(defn path-exists?
  ([path]
     (path-exists? "" path))
  ([user path]
    (let [path (ft/rm-last-slash path)]
      (with-jargon (jargon-cfg) [cm]
        (exists? cm (url-decode path))))))

(defn do-exists
  [{user :user} {paths :paths}]
  {:paths
   (apply
     conj {}
     (map #(hash-map %1 (path-exists? user %1)) paths))})

(with-pre-hook! #'do-exists
  (fn [params body]
    (log-call "do-exists" params)
    (validate-map params {:user string?})
    (validate-map body {:paths vector?})
    (validate-num-paths (:paths body))))

(with-post-hook! #'do-exists (log-func "do-exists"))

(defn entry-accessible?
  "Indicates if a filesystem entry is readble by a given user.

   Parameters:
     cm - The open Jargon context for the filesystem
     user - the authenticated name of the user
     entry-id the UUID of the filesystem entry"
  [cm user entry-id]
  (let [entry-path (:path (uuid/path-for-uuid cm user (str entry-id)))]
    (and entry-path (fs-perm/is-readable? cm user entry-path))))


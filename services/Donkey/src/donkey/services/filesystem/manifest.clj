(ns donkey.services.filesystem.manifest
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.validators]
        [donkey.services.filesystem.sharing :only [anon-file-url anon-readable?]]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-ops :only [input-stream]]
        [clj-jargon.metadata :only [get-attribute attribute?]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.services.filesystem.validators :as validators]
            [donkey.services.filesystem.stat :refer [detect-content-type]]
            [donkey.services.filesystem.garnish.irods :as filetypes]
            [ring.util.codec :as cdc]
            [donkey.clients.tree-urls :as tree]
            [donkey.util.config :as cfg]
            [donkey.services.filesystem.icat :as icat])
  (:import [org.apache.tika Tika]))

(def ^:private coge-attr "ipc-coge-link")

(defn- extract-tree-urls
  [cm fpath]
  (if (attribute? cm fpath "tree-urls")
    (-> (get-attribute cm fpath "tree-urls")
      first
      :value
      ft/basename
      tree/get-tree-urls
      :tree-urls)
    []))

(defn- extract-coge-view
  [cm fpath]
  (if (attribute? cm fpath coge-attr)
    (mapv (fn [{url :value} idx] {:label (str "gene_" idx) :url url})
          (get-attribute cm fpath coge-attr) (range))
    []))

(defn- format-anon-files-url
  [fpath]
  {:label "anonymous" :url (anon-file-url fpath)})

(defn- extract-urls
  [cm fpath]
  (let [urls (concat (extract-tree-urls cm fpath) (extract-coge-view cm fpath))]
    (into [] (if (anon-readable? cm fpath)
               (conj urls (format-anon-files-url fpath))
               urls))))

(defn- manifest-map
  [cm user path]
  {:action       "manifest"
   :content-type (detect-content-type cm path)
   :urls         (extract-urls cm path)
   :info-type    (filetypes/get-types cm user path)})

(defn- manifest
  [user path data-threshold]
  (let [path (ft/rm-last-slash path)]
    (with-jargon (icat/jargon-cfg) [cm]
      (validators/user-exists cm user)
      (validators/path-exists cm path)
      (validators/path-is-file cm path)
      (validators/path-readable cm user path)
      (manifest-map cm user path))))

(defn do-manifest
  [{user :user path :path}]
  (manifest user path (cfg/fs-data-threshold)))

(with-pre-hook! #'do-manifest
  (fn [params]
    (log-call "do-manifest" params)))

(with-post-hook! #'do-manifest (log-func "do-manifest"))

(ns donkey.services.filesystem.manifest
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.util.config]
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
            [donkey.services.filesystem.riak :as riak]
            [donkey.services.garnish.irods :as filetypes]
            [ring.util.codec :as cdc])
  (:import [org.apache.tika Tika]))

(def ^:private coge-attr "ipc-coge-link")

(defn- preview-url
  [user path]
  (str "file/preview?user=" (cdc/url-encode user) "&path=" (cdc/url-encode path)))

(defn- content-type
  [cm path]
  (.detect (Tika.) (input-stream cm path)))

(defn- extract-tree-urls
  [cm fpath]
  (if (attribute? cm fpath "tree-urls")
    (-> (get-attribute cm fpath "tree-urls")
        first
        :value
        riak/get-tree-urls
        (json/decode true)
        :tree-urls)
    []))

(defn- extract-coge-view
  [cm fpath]
  (if (attribute? cm fpath coge-attr)
    (mapv (fn [{url :value} idx] {:label (str "gene_" idx) :url url})
          (get-attribute cm fpath coge-attr) (range))
    []))

(defn- extract-urls
  [cm fpath]
  (into [] (concat (extract-tree-urls cm fpath) (extract-coge-view cm fpath))))

(defn- manifest-map
  [cm user path]
  {:action       "manifest"
   :content-type (content-type cm path)
   :urls         (extract-urls cm path)
   :info-type    (filetypes/get-types cm user path)
   :mime-type    (.detect (Tika.) (input-stream cm path))
   :preview      (preview-url user path)})

(defn- manifest
  [user path data-threshold]
  (let [path (ft/rm-last-slash path)]
    (with-jargon (jargon-cfg) [cm]
      (validators/user-exists cm user)
      (validators/path-exists cm path)
      (validators/path-is-file cm path)
      (validators/path-readable cm user path)

      (if (anon-readable? cm path)
        (merge {:anon-url (anon-file-url path)}
               (manifest-map cm user path))
        (manifest-map cm user path)))))

(defn do-manifest
  [{user :user path :path}]
  (manifest user path (fs-data-threshold)))

(with-pre-hook! #'do-manifest
  (fn [params]
    (log-call "do-manifest" params)))

(with-post-hook! #'do-manifest (log-func "do-manifest"))

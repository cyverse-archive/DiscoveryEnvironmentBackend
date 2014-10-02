(ns data-info.services.updown
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [data-info.services.common-paths]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.cart]
        [clj-jargon.item-info :only [file-size]]
        [clj-jargon.item-ops :only [input-stream]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.services.directory :as directory]
            [data-info.services.validators :as validators]
            [clj-icat-direct.icat :as icat]
            [data-info.util.config :as cfg]
            [data-info.services.icat :as jargon])
  (:import [org.apache.tika Tika]))

(defn- tika-detect-type
  [user file-path]
  (with-jargon (jargon/jargon-cfg) [cm-new]
    (validators/user-exists cm-new user)
    (validators/path-exists cm-new file-path)
    (validators/path-readable cm-new user file-path)
    (.detect (Tika.) (input-stream cm-new file-path))))

(defn- download-file
  [user file-path]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm file-path)
    (validators/path-readable cm user file-path)
    (if (zero? (file-size cm file-path)) "" (input-stream cm file-path))))

(defn- download
  [user filepaths]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [cart-key (str (System/currentTimeMillis))
          account  (:irodsAccount cm)]
      {:action "download"
       :status "success"
       :data
       {:user user
        :home (ft/path-join "/" (cfg/irods-zone) "home" user)
        :password (store-cart cm user cart-key filepaths)
        :host (.getHost account)
        :port (.getPort account)
        :zone (.getZone account)
        :defaultStorageResource (.getDefaultStorageResource account)
        :key cart-key}})))

(defn- upload
  [user]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [account (:irodsAccount cm)]
      {:action "upload"
       :status "success"
       :data
       {:user user
        :home (ft/path-join "/" (cfg/irods-zone) "home" user)
        :password (temp-password cm user)
        :host (.getHost account)
        :port (.getPort account)
        :zone (.getZone account)
        :defaultStorageResource (.getDefaultStorageResource account)
        :key (str (System/currentTimeMillis))}})))

(defn do-download
  [{user :user} {paths :paths}]
  (download user paths))

(with-pre-hook! #'do-download
  (fn [params body]
    (log-call "do-download" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential?})))

(with-post-hook! #'do-download (log-func "do-download"))

(defn do-download-contents
  [{user :user} {path :path}]
  (let [limit (:total (icat/number-of-items-in-folder user (cfg/irods-zone) path)) ;; FIXME this is horrible
        paths (directory/get-paths-in-folder user path limit)]
    (download user paths)))

(with-pre-hook! #'do-download-contents
  (fn [params body]
    (log-call "do-download-contents" params body)
    (validate-map params {:user string?})
    (validate-map body {:path string?})
    (with-jargon (jargon/jargon-cfg) [cm] (validators/path-is-dir cm (:path body)))))

(with-post-hook! #'do-download-contents (log-func "do-download-contents"))

(defn do-upload
  [{user :user}]
  (upload user))

(with-pre-hook! #'do-upload
  (fn [params]
    (log-call "do-upload" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-upload (log-func "do-upload"))

(defn- attachment?
  [params]
  (if-not (contains? params :attachment)
    true
    (if (= "1" (:attachment params)) true false)))

(defn- get-disposition
  [params]
  (cond
    (not (contains? params :attachment))
    (str "attachment; filename=\"" (ft/basename (:path params)) "\"")

    (not (attachment? params))
    (str "filename=\"" (ft/basename (:path params)) "\"")

    :else
    (str "attachment; filename=\"" (ft/basename (:path params)) "\"")))

(defn do-special-download
  [{user :user path :path :as params}]
  (let [content      (download-file user path)
        content-type @(future (tika-detect-type user path))
        disposition  (get-disposition params)]
    {:status               200
     :body                 content
     :headers {"Content-Disposition" disposition
               "Content-Type"        content-type}}))

(with-pre-hook! #'do-special-download
  (fn [params]
    (log-call "do-special-download" params)
    (validate-map params {:user string? :path string?})
    (let [user (:user params)
          path (:path params)]
      (log/info "User for download: " user)
      (log/info "Path to download: " path)

      (when (super-user? user)
        (throw+ {:error_code ERR_NOT_AUTHORIZED
                 :user       user})))))

(with-post-hook! #'do-special-download (log-func "do-special-download"))

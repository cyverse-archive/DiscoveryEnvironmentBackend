(ns data-info.services.updown
  (:require [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [slingshot.slingshot :refer [throw+]]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.cart :as cart]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :refer [file-size]]
            [clj-jargon.item-ops :refer [input-stream]]
            [clojure-commons.error-codes :as error]
            [clojure-commons.file-utils :as ft]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.services.common-paths :as path]
            [data-info.services.directory :as directory]
            [data-info.services.icat :as jargon]
            [data-info.services.type-detect.irods :as type]
            [data-info.services.validators :as validators]))


(defn- download-file
  [user file-path]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm file-path)
    (validators/path-readable cm user file-path)
    (if (zero? (file-size cm file-path))
      ""
      (input-stream cm file-path))))


(defn- download
  [user filepaths]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [cart-key (str (System/currentTimeMillis))
          account  (:irodsAccount cm)]
      {:action "download"
       :status "success"
       :data   {:user                   user
                :home                   (path/user-home-dir user)
                :password               (cart/store-cart cm user cart-key filepaths)
                :host                   (.getHost account)
                :port                   (.getPort account)
                :zone                   (.getZone account)
                :defaultStorageResource (.getDefaultStorageResource account)
                :key                    cart-key}})))


(defn- upload
  [user]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [account (:irodsAccount cm)]
      {:action "upload"
       :status "success"
       :data   {:user                   user
                :home                   (path/user-home-dir user)
                :password               (cart/temp-password cm user)
                :host                   (.getHost account)
                :port                   (.getPort account)
                :zone                   (.getZone account)
                :defaultStorageResource (.getDefaultStorageResource account)
                :key                    (str (System/currentTimeMillis))}})))


(defn- do-download
  [{user :user} {paths :paths}]
  (download user paths))

(with-pre-hook! #'do-download
  (fn [params body]
    (path/log-call "do-download" params body)
    (cv/validate-map params {:user string?})
    (cv/validate-map body {:paths sequential?})))

(with-post-hook! #'do-download (path/log-func "do-download"))


(defn- do-download-contents
  [{user :user path :path}]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/path-is-dir cm path))
  (download user (directory/get-paths-in-folder user path)))

(with-pre-hook! #'do-download-contents
  (fn [params]
    (path/log-call "do-download-contents" params)
    (cv/validate-map params {:user string? :path string?})))

(with-post-hook! #'do-download-contents (path/log-func "do-download-contents"))


(defn dispatch-download
  [{dir :dir :as params} body]
  (if dir
    (do-download params body)
    (do-download-contents params)))


(defn do-upload
  [{user :user}]
  (upload user))

(with-pre-hook! #'do-upload
  (fn [params]
    (path/log-call "do-upload" params)
    (cv/validate-map params {:user string?})))

(with-post-hook! #'do-upload (path/log-func "do-upload"))


(defn- get-disposition
  [path attachment]
  (if (or (nil? attachment) (Boolean/parseBoolean attachment))
    (str "attachment; filename=\"" (ft/basename path) "\"")
    (str "filename=\"" (ft/basename path) "\"")))


(defn do-special-download
  [path {user :user attachment :attachment}]
  (when (path/super-user? user)
    (throw+ {:error_code error/ERR_NOT_AUTHORIZED :user user}))
  (let [content-type (future (type/detect-media-type path))]
    {:status  200
     :body    (download-file user path)
     :headers {"Content-Disposition" (get-disposition path attachment)
               "Content-Type"        @content-type}}))

(with-pre-hook! #'do-special-download
  (fn [path params]
    (path/log-call "do-special-download" path params)
    (cv/validate-map params {:user string?})
    (when-let [attachment (:attachment params)]
      (validators/valid-bool-param "attachment" attachment))
    (log/info "User for download: " (:user params))
    (log/info "Path to download: " path)))

(with-post-hook! #'do-special-download (path/log-func "do-special-download"))

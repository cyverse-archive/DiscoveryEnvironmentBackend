(ns data-info.services.updown
  (:require [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [slingshot.slingshot :refer [throw+]]
            [clj-icat-direct.icat :as icat]
            [clj-jargon.cart :as cart]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :as item]
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


(defn- abs-path
  [zone path-in-zone]
  (ft/path-join "/" zone path-in-zone))


(defn- gather-paths
  [cm user folder other-paths]
  (when folder
    (validators/path-is-dir cm folder))
  (set (concat other-paths
               (when folder
                 (directory/get-paths-in-folder user folder)))))


(defn- mk-cart
  [cart-key user password]
  {:key                    cart-key
   :user                   user
   :home                   (path/user-home-dir user)
   :password               password
   :host                   (cfg/irods-host)
   :port                   (cfg/irods-port)
   :zone                   (cfg/irods-zone)
   :defaultStorageResource (cfg/irods-resc)})


(defn dispatch-cart
  [{folder :folder user :user} {other-paths :paths}]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (let [paths    (gather-paths cm user folder other-paths)
          cart-key (str (System/currentTimeMillis))
          password (if (empty? paths)
                     (cart/temp-password cm user)
                     (cart/store-cart cm user cart-key paths))]
      {:cart (mk-cart cart-key user password)})))


(with-pre-hook! #'dispatch-cart
  (fn [params body]
    (path/log-call "dispatch-cart" params body)
    (cv/validate-map params {:user string?})
    (if-not (empty? body)
      (cv/validate-map body {:paths sequential?}))))

(with-post-hook! #'dispatch-cart (path/log-func "dispatch-cart"))


(defn- download-file
  [user file-path]
  (with-jargon (jargon/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm file-path)
    (validators/path-readable cm user file-path)
    (if (zero? (file-size cm file-path))
      ""
      (input-stream cm file-path))))


(defn- get-disposition
  [path attachment]
  (let [filename (str \" (ft/basename path) \")]
    (if (or (nil? attachment) (Boolean/parseBoolean attachment))
      (str "attachment; filename=" filename)
      (str "filename=" filename))))


(defn do-special-download
  [path {:keys [attachment user zone]}]
  (when (path/super-user? user)
    (throw+ {:error_code error/ERR_NOT_AUTHORIZED :user user}))
  (let [path         (ft/path-join "/" zone path)
        content-type (future (type/detect-media-type path))]
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

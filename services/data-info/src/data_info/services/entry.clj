(ns data-info.services.entry
  "This namespace provides the business logic for all entries endpoints."
  (:require [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.init :as init]
            [clj-jargon.item-info :as item]
            [clj-jargon.item-ops :as ops]
            [clj-jargon.permissions :as perm]
            [clojure-commons.file-utils :as file]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.irods :as irods]
            [data-info.util.validators :as duv]
            [data-info.services.directory :as dir])
  (:import [java.util UUID]))


(defn id-exists?
  [{user :user entry :entry}]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (duv/user-exists cm user)
    (if-let [path (irods/get-path cm (UUID/fromString entry))]
      {:status (if (perm/is-readable? cm user path) 200 403)}
      {:status 404})))


(with-pre-hook! #'id-exists?
  (fn [params]
    (dul/log-call "exists?" params)
    (duv/valid-uuid-param "entry" (:entry params))
    (cv/validate-map params {:user string?})))

(with-post-hook! #'id-exists? (dul/log-func "exists?"))


(defn- abs-path
  [zone path-in-zone]
  (file/path-join "/" zone path-in-zone))


(defn- download-file
  [user file-path]
  (init/with-jargon (cfg/jargon-cfg) [cm]
    (duv/path-readable cm user file-path)
    (if (zero? (item/file-size cm file-path))
      ""
      (ops/input-stream cm file-path))))


(defn- get-disposition
  [path attachment]
  (let [filename (str \" (file/basename path) \")]
    (if (or (nil? attachment) (Boolean/parseBoolean attachment))
      (str "attachment; filename=" filename)
      (str "filename=" filename))))


(defn- get-file
  [path {:keys [attachment user]}]
  (let [content-type (future (irods/detect-media-type path))]
    {:status  200
     :body    (download-file user path)
     :headers {"Content-Disposition" (get-disposition path attachment)
               "Content-Type"        @content-type}}))

(with-pre-hook! #'get-file
  (fn [path params]
    (dul/log-call "do-special-download" path params)
    (cv/validate-map params {:user string?})
    (when-let [attachment (:attachment params)]
      (duv/valid-bool-param "attachment" attachment))
    (log/info "User for download: " (:user params))
    (log/info "Path to download: " path)))

(with-post-hook! #'get-file (dul/log-func "do-special-download"))


(defn get-by-path
  [path-in-zone {zone :zone :as params}]
  (let [full-path (abs-path zone path-in-zone)

        ;; detecting if the path is a folder happens in a separate connection to iRODS on purpose.
        ;; It appears that downloading a file after detecting its type causes the download to fail.
        ; TODO after migrating to jargon 4, check to see if this error still occurs.
        folder? (init/with-jargon (cfg/jargon-cfg) [cm]
                  (duv/path-exists cm full-path)
                  (item/is-dir? cm full-path))]
    (if folder?
      (dir/do-paged-listing full-path params)
      (get-file full-path params))))

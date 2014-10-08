(ns data-info.services.entry
  "This namespace provides the business logic for all entries endpoints."
  (:require [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.init :as init]
            [clj-jargon.item-info :as item]
            [clj-jargon.permissions :as perm]
            [clojure-commons.file-utils :as file]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as log]
            [data-info.util.irods :as irods]
            [data-info.util.validators :as duv]
            [data-info.services.directory :as dir]
            [data-info.services.updown :as updown])
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
    (log/log-call "exists?" params)
    (duv/valid-uuid-param "entry" (:entry params))
    (cv/validate-map params {:user string?})))

(with-post-hook! #'id-exists? (log/log-func "exists?"))


(defn- abs-path
  [zone path-in-zone]
  (file/path-join "/" zone path-in-zone))


(defn get-by-path
  [path-in-zone {zone :zone :as params}]
  (let [full-path (abs-path zone path-in-zone)
        ;; detecting if the path is a folder happens in a separate connection to iRODS on purpose.
        ;; It appears that downloading a file after detecting its type causes the download to fail.
        ; TODO after migrating to jargon 4, check to see if this error still occurs.
        folder?   (init/with-jargon (cfg/jargon-cfg) [cm]
                    (duv/path-exists cm full-path)
                    (item/is-dir? cm full-path))]
    (if folder?
      (dir/do-paged-listing full-path params)
      (updown/do-special-download full-path params))))

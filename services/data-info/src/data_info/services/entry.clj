(ns data-info.services.entry
  "This namespace provides the business logic for all entries endpoints."
  (:require [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.init :as init]
            [clj-jargon.permissions :as perm]
            [clojure-commons.validators :as cv]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as log]
            [data-info.util.irods :as irods]
            [data-info.util.validators :as duv]
            [data-info.services.exists :as exists]
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


(defn get-by-path
  [path-in-zone params]
  (updown/dispatch-entries-path path-in-zone params))

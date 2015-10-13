(ns data-info.services.page-file
  (:use [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info]
        [clj-jargon.paging]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.services.uuids :as uuids]
            [data-info.util.config :as cfg]
            [data-info.util.logging :as dul]
            [data-info.util.validators :as validators]))

(defn- read-file-chunk
  "Reads a chunk of a file starting at 'position' and reading a chunk of length 'chunk-size'."
  [user path position chunk-size]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-is-file cm path)
    (validators/path-readable cm user path)
    {:path       path
     :user       user
     :start      (str position)
     :chunk-size (str chunk-size)
     :file-size  (str (file-size cm path))
     :chunk      (read-at-position cm path position chunk-size)}))

(defn do-read-chunk
  [{user :user position :position chunk-size :size} data-id]
  (let [path (ft/rm-last-slash (:path (uuids/path-for-uuid user data-id)))]
    (read-file-chunk user path position chunk-size)))

(with-pre-hook! #'do-read-chunk
  (fn [params data-id]
    (dul/log-call "do-read-chunk" params data-id)))

(with-post-hook! #'do-read-chunk (dul/log-func "do-read-chunk"))

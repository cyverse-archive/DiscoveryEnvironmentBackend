(ns data-info.services.filesystem.page-file
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [data-info.services.filesystem.common-paths]
        [data-info.services.filesystem.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info]
        [clj-jargon.paging]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.services.filesystem.icat :as cfg]
            [data-info.services.filesystem.validators :as validators]))

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

(defn- overwrite-file-chunk
  "Writes a chunk of a file starting at 'position' and extending to the length of the string."
  [user path position update-string]
  (with-jargon (cfg/jargon-cfg) [cm]
    (validators/user-exists cm user)
    (validators/path-exists cm path)
    (validators/path-is-file cm path)
    (validators/path-writeable cm user path)
    (overwrite-at-position cm path position update-string)
    {:path       path
     :user       user
     :start      (str position)
     :chunk-size (str (count (.getBytes update-string)))
     :file-size  (str (file-size cm path))}))

(defn do-read-chunk
  [{user :user} {path :path position :position chunk-size :chunk-size}]
  (let [pos  (Long/parseLong position)
        size (Long/parseLong chunk-size)]
    (read-file-chunk user path pos size)))

(with-pre-hook! #'do-read-chunk
  (fn [params body]
    (log-call "do-read-chunk" params body)
    (validate-map params {:user string?})
    (validate-map body {:path string? :position string? :chunk-size string?})))

(with-post-hook! #'do-read-chunk (log-func "do-read-chunk"))

(defn do-overwrite-chunk
  [{user :user} {path :path position :position update :update}]
  (let [pos  (Long/parseLong position)]
    (overwrite-file-chunk user path pos update)))

(with-pre-hook! #'do-overwrite-chunk
  (fn [params body]
    (log-call "do-overwrite-chunk" params body)
    (validate-map params {:user string?})
    (validate-map body {:path string? :position string? :update string?})))

(with-post-hook! #'do-overwrite-chunk (log-func "do-overwrite-chunk"))

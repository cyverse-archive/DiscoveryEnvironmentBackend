(ns donkey.services.filesystem.page-file
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.services.filesystem.common-paths]
        [donkey.services.filesystem.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info]
        [clj-jargon.paging]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.services.filesystem.icat :as cfg]
            [donkey.services.filesystem.validators :as validators]))

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

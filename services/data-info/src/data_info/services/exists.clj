(ns data-info.services.exists
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [data-info.services.common-paths]
        [data-info.services.validators]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-info :only [exists?]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]
            [cheshire.core :as json]
            [cemerick.url :as url]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [data-info.services.icat :as cfg]
            [data-info.services.validators :as validators]))

(defn- url-encoded?
  [string-to-check]
  (re-seq #"\%[A-Fa-f0-9]{2}" string-to-check))

(defn- url-decode
  [string-to-decode]
  (if (url-encoded? string-to-decode)
    (url/url-decode string-to-decode)
    string-to-decode))

(defn path-exists?
  ([path]
     (path-exists? "" path))
  ([user path]
    (let [path (ft/rm-last-slash path)]
      (with-jargon (cfg/jargon-cfg) [cm]
        (exists? cm (url-decode path))))))

(defn do-exists
  [{user :user} {paths :paths}]
  {:paths
   (apply
     conj {}
     (map #(hash-map %1 (path-exists? user %1)) paths))})

(with-pre-hook! #'do-exists
  (fn [params body]
    (log-call "do-exists" params)
    (validate-map params {:user string?})
    (validate-map body {:paths vector?})
    (validate-num-paths (:paths body))))

(with-post-hook! #'do-exists (log-func "do-exists"))

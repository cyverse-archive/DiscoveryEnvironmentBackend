(ns data-info.services.exists
  (:require [cemerick.url :as url]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clj-jargon.init :refer [with-jargon]]
            [clj-jargon.item-info :as item]
            [clj-jargon.permissions :as perm]
            [clojure-commons.file-utils :as ft]
            [clojure-commons.validators :as cv]
            [data-info.services.common-paths :as paths]
            [data-info.services.icat :as cfg]
            [data-info.services.validators :as dsv]))


(defn- url-encoded?
  [string-to-check]
  (re-seq #"\%[A-Fa-f0-9]{2}" string-to-check))


(defn- url-decode
  [string-to-decode]
  (if (url-encoded? string-to-decode)
    (url/url-decode string-to-decode)
    string-to-decode))


(defn- path-exists-for-user?
  [user path]
  (let [path (ft/rm-last-slash (url-decode path))]
    (with-jargon (cfg/jargon-cfg) [cm]
      (and (item/exists? cm path)
           (perm/is-readable? cm user path)))))


(defn do-exists
  [{user :user} {paths :paths}]
  {:paths (into {}
                (map #(hash-map % (path-exists-for-user? user %)) (set paths)))})

(with-pre-hook! #'do-exists
  (fn [params body]
    (paths/log-call "do-exists" params)
    (cv/validate-map params {:user string?})
    (cv/validate-map body {:paths vector?})
    (dsv/validate-num-paths (:paths body))))

(with-post-hook! #'do-exists (paths/log-func "do-exists"))


(defn exists?
  [entry {user :user}]
  (with-jargon (cfg/jargon-cfg) [cm]
    (if-not (item/exists? cm entry)
      {:status 404}
      (if-not (perm/is-readable? cm user entry)
        {:status 403}
        {:status 200}))))

(with-pre-hook! #'exists?
  (fn [entry params]
    (paths/log-call "exists?" entry params)
    (cv/validate-map params {:user string?})
    (with-jargon (cfg/jargon-cfg) [cm]
      (dsv/user-exists cm (:user params)))))

(with-post-hook! #'exists? (paths/log-func "exists?"))

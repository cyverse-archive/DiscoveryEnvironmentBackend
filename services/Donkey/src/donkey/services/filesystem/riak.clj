(ns donkey.services.filesystem.riak
  (:use [donkey.util.config]
        [clojure-commons.error-codes]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cemerick.url :as curl] 
            [clj-http.client :as cl]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]))

(defn- key-url
  [url-path]
  (str (curl/url (fs-riak-url) url-path) "?returnbody=true"))

(defn- request-failed
  [resp]
  (throw+ {:error_codes ERR_REQUEST_FAILED
           :body (:body resp)}))

(defn get-tree-urls
  [url-path]
  (let [resp (cl/get (key-url url-path) {:throw-exceptions false})]
    (cond
     (<= 200 (:status resp) 299) (:body resp)
     (= 404 (:status resp))      "{\"tree-urls\" : []}"
     :else                       (request-failed resp))))


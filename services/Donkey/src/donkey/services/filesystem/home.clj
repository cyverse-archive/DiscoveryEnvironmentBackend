(ns donkey.services.filesystem.home
  (:use [clojure-commons.validators]
        [donkey.services.filesystem.common-paths])
  (:require [cemerick.url :as url]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.util.config :as cfg]))


(defn do-homedir
  [{user :user}]
  (let [url       (url/url (cfg/data-info-base-url) "home")
        req-map   {:query-params {:user user}}
        resp      (http/get (str url) req-map)
        home-path (:path (json/decode (:body resp) true))]
    {:id   (str "/root" home-path)
     :path home-path}))

(with-pre-hook! #'do-homedir
  (fn [params]
    (log-call "do-homedir" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-homedir (log-func "do-homedir"))

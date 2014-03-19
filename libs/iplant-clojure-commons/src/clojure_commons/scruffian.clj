(ns clojure-commons.scruffian
  (:require [clj-http.client :as client]
            [clojure-commons.client :as cc]))

(defn download
  "Downloads a file from Scruffian."
  [base user path]
  (let [url (cc/build-url base "download")
        res (cc/get url {:as           :stream
                         :query-params {:path path
                                        :user user}})]
    (:body res)))

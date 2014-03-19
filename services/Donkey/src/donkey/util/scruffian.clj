(ns donkey.util.scruffian
  (:require [clj-http.client :as client]
            [clojure-commons.client :as cc]
            [donkey.services.fileio.actions :as fileio]))

(defn download
  "Downloads a file from Scruffian."
  [user path]
  (:body (fileio/download user path)))

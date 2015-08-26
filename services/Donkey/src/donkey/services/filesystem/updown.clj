(ns donkey.services.filesystem.updown
  (:use [clojure-commons.validators]
        [donkey.services.filesystem.common-paths]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clojure-commons.error-codes :as error]
            [clojure-commons.file-utils :as ft]
            [donkey.clients.data-info :as data]))


(defn- download-file
  [user file]
  (let [url-path         (data/mk-data-path-url-path file)
        req-map          {:query-params {:user user} :as :stream}
        handle-not-found (fn [_ _ _] (throw+ {:error_code error/ERR_NOT_FOUND :path file}))]
    (data/request :get url-path req-map
      :403 handle-not-found
      :404 handle-not-found
      :410 handle-not-found
      :414 handle-not-found)))


(defn- attachment?
  [params]
  (if-not (contains? params :attachment)
    true
    (if (= "1" (:attachment params)) true false)))


(defn- get-disposition
  [params]
  (cond
    (not (contains? params :attachment))
    (str "attachment; filename=\"" (ft/basename (:path params)) "\"")

    (not (attachment? params))
    (str "filename=\"" (ft/basename (:path params)) "\"")

    :else
    (str "attachment; filename=\"" (ft/basename (:path params)) "\"")))


(defn do-special-download
  [{user :user path :path :as params}]
  (let [resp (download-file user path)]
    {:status  200
     :body    (:body resp)
     :headers {"Content-Disposition" (get-disposition params)
               "Content-Type"        (get-in resp [:headers "Content-Type"])}}))

(with-pre-hook! #'do-special-download
  (fn [params]
    (log-call "do-special-download" params)
    (validate-map params {:user string? :path string?})
    (let [user (:user params)
          path (:path params)]
      (log/info "User for download: " user)
      (log/info "Path to download: " path)

      (when (super-user? user)
        (throw+ {:error_code error/ERR_NOT_AUTHORIZED :user user})))))

(with-post-hook! #'do-special-download (log-func "do-special-download"))

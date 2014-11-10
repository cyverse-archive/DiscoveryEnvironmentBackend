(ns donkey.services.filesystem.updown
  (:use [clojure-commons.validators]
        [donkey.services.filesystem.common-paths]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [clojure-commons.error-codes :as error]
            [clojure-commons.file-utils :as ft]
            [donkey.clients.data-info :as data]))


(defn- fmt-cart-response
  [action response]
  {:data   (json/decode (:body response) true)})


(defn- handle-unprocessable-cart
  [method url err]
  (let [body (json/decode (:body err) true)]
    (if (= (:error_code body) error/ERR_NOT_A_FOLDER)
      (throw+ body)
      (data/respond-with-default-error method url err))))


(defn do-download
  [{user :user} {paths :paths}]
  (let [req-map {:query-params {:user user}
                 :content-type :json
                 :body         (json/encode {:paths paths})}
        resp    (data/request :post "cart" req-map)]
    (fmt-cart-response "download" resp)))

(with-pre-hook! #'do-download
  (fn [params body]
    (log-call "do-download" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential?})))

(with-post-hook! #'do-download (log-func "do-download"))


(defn do-download-contents
  [{user :user} {folder :path}]
  (let [req-map {:query-params {:folder folder :user user}}
        resp    (data/request :post "cart" req-map :422 handle-unprocessable-cart)]
    (fmt-cart-response "download" resp)))

(with-pre-hook! #'do-download-contents
  (fn [params body]
    (log-call "do-download-contents" params body)
    (validate-map params {:user string?})
    (validate-map body {:path string?})))

(with-post-hook! #'do-download-contents (log-func "do-download-contents"))


(defn do-upload
  [{user :user}]
  (fmt-cart-response "upload" (data/request :post "cart" {:query-params {:user user}})))

(with-pre-hook! #'do-upload
  (fn [params]
    (log-call "do-upload" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-upload (log-func "do-upload"))


(defn- download-file
  [user file]
  (let [url-path         (data/mk-entries-path-url-path file)
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
               "Content-Type"        (:content-type resp)}}))

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

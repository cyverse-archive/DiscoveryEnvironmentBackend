(ns donkey.services.filesystem.updown
  (:use [clojure-commons.error-codes]
        [clojure-commons.validators]
        [donkey.services.filesystem.common-paths]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [donkey.clients.data-info :as data]))


(defn do-download
  [{user :user} {paths :paths}]
  {:action "download"
   :status "success"
   :data   (data/make-a-la-cart user paths)})

(with-pre-hook! #'do-download
  (fn [params body]
    (log-call "do-download" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential?})))

(with-post-hook! #'do-download (log-func "do-download"))


(defn do-download-contents
  [{user :user} {path :path}]
  {:action "download"
   :status "success"
   :data   (data/make-folder-cart user path)})

(with-pre-hook! #'do-download-contents
  (fn [params body]
    (log-call "do-download-contents" params body)
    (validate-map params {:user string?})
    (validate-map body {:path string?})))

(with-post-hook! #'do-download-contents (log-func "do-download-contents"))


(defn do-upload
  [{user :user}]
  {:action "upload"
   :status "success"
   :data   (data/make-empty-cart user)})

(with-pre-hook! #'do-upload
  (fn [params]
    (log-call "do-upload" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-upload (log-func "do-upload"))

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
  (let [resp (data/download-file user path)]
    {:status  200
     :body    (:file-stream resp)
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
        (throw+ {:error_code ERR_NOT_AUTHORIZED
                 :user       user})))))

(with-post-hook! #'do-special-download (log-func "do-special-download"))

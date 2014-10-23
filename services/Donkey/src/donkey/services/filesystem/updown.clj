(ns donkey.services.filesystem.updown
  (:use [clojure-commons.validators]
        [donkey.services.filesystem.common-paths]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [cemerick.url :as url]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [dire.core :refer [with-pre-hook! with-post-hook!]]
            [me.raynes.fs :as fs]
            [clojure-commons.error-codes :as error]
            [clojure-commons.file-utils :as ft]
            [donkey.util.config :as cfg]
            [donkey.util.service :as svc]))


(defn- handle-data-info-error
  [& msg]
  (let [full-msg (apply string/join (cfg/data-info-base-url) "had a service error:" msg)]
    (log/error full-msg)
    (svc/request-failure full-msg)))


(defn- handle-internal-error
  [err & msg]
  (let [full-msg (apply string/join "internal error:" msg)]
    (log/error err full-msg)
    (svc/request-failure full-msg)))


(defn- handle-unprocessable-cart
  [err resource handle-not-a-folder]
  (let [body (json/decode (:body err) true)]
    (if (and handle-not-a-folder
          (= (:error_code body) error/ERR_NOT_A_FOLDER))
      (handle-not-a-folder body)
      (handle-internal-error err "unprocessable entity provided to" resource))))


(defn- exec-cart-query
  [req-map & [handle-not-a-folder]]
  (let [url-str (str (url/url (cfg/data-info-base-url) "cart"))]
    (try+
      (:body (http/post url-str (assoc req-map :as :json)))
      (catch [:status 400] err
        (handle-internal-error err "bad request to" url-str))
      (catch [:status 401] err
        (handle-internal-error err "haven't authorized for" url-str))
      (catch [:status 403] err
        (handle-internal-error err "not allowed to use" url-str))
      (catch [:status 404] err
        (handle-internal-error err url-str "not found"))
      (catch [:status 405] err
        (handle-internal-error err url-str "doesn't support POST"))
      (catch [:status 406] err
        (handle-internal-error err url-str "cannot generate acceptable content"))
      (catch [:status 409] err
        (handle-internal-error err url-str "conflicted"))
      (catch [:status 410] err
        (handle-internal-error err url-str "no longer exists"))
      (catch [:status 412] err
        (handle-internal-error err url-str "precondition failed"))
      (catch [:status 413] err
        (handle-internal-error err "request to" url-str "too large"))
      (catch [:status 414] err
        (handle-internal-error err "URI too long:" url-str))
      (catch [:status 415] err
        (handle-internal-error err url-str "does not serve JSON content"))
      (catch [:status 422] err
        (handle-unprocessable-cart err url-str handle-not-a-folder))
      (catch [:status 500] err
        (handle-data-info-error "had an internal error related to the /cart resource"))
      (catch [:status 501] err
        (handle-data-info-error "has not implemented POST /cart"))
      (catch [:status 503] err
        (handle-data-info-error "temporarily unavailable")))))


(defn do-download
  [{user :user} {paths :paths}]
  (let [cart-info (exec-cart-query {:query-params {:user user}
                                    :content-type :json
                                    :body         (json/encode {:paths paths})})]
    {:action "download"
     :status "success"
     :data   cart-info}))

(with-pre-hook! #'do-download
  (fn [params body]
    (log-call "do-download" params body)
    (validate-map params {:user string?})
    (validate-map body {:paths sequential?})))

(with-post-hook! #'do-download (log-func "do-download"))


(defn do-download-contents
  [{user :user} {folder :path}]
  (let [cart-info (exec-cart-query {:query-params {:folder folder :user user}}
                                   #(throw+ %))]
    {:action "download"
     :status "success"
     :data   cart-info}))

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
   :data   (exec-cart-query {:query-params {:user user}})})

(with-pre-hook! #'do-upload
  (fn [params]
    (log-call "do-upload" params)
    (validate-map params {:user string?})))

(with-post-hook! #'do-upload (log-func "do-upload"))


(defn- download-file
  [user file]
  (let [nodes   (map url/url-encode (next (fs/split file)))
        url-str (str (apply url/url (cfg/data-info-base-url) "entries" "path" nodes))]
    (try+
      (http/get url-str
                {:query-params {:user user}
                 :as           :stream})
      (catch [:status 400] err
        (handle-internal-error err "bad request to" url-str))
      (catch [:status 401] err
        (handle-internal-error err "haven't authorized for" url-str))
      (catch [:status 403] err
        (throw+ {:error_code error/ERR_NOT_FOUND :path file}))
      (catch [:status 404] err
        (throw+ {:error_code error/ERR_NOT_FOUND :path file}))
      (catch [:status 405] err
        (handle-internal-error err url-str "doesn't support GET"))
      (catch [:status 406] err
        (handle-internal-error err url-str "cannot generate acceptable content"))
      (catch [:status 409] err
        (handle-internal-error err url-str "conflicted"))
      (catch [:status 410] err
        (handle-internal-error err url-str "no longer exists"))
      (catch [:status 412] err
        (handle-internal-error err url-str "precondition failed"))
      (catch [:status 413] err
        (handle-internal-error err "request to" url-str "too large"))
      (catch [:status 414] err
        (handle-internal-error err "URI too long:" url-str))
      (catch [:status 415] err
        (handle-internal-error err url-str "does not serve JSON content"))
      (catch [:status 422] err
        (handle-internal-error err "unprocessable entity provided to" url-str))
      (catch [:status 500] err
        (handle-data-info-error "had an internal error related to the /cart resource"))
      (catch [:status 501] err
        (handle-data-info-error "has not implemented POST /cart"))
      (catch [:status 503] err
        (handle-data-info-error "temporarily unavailable")))))


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

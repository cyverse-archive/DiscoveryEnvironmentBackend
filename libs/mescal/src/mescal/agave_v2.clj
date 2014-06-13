(ns mescal.agave-v2
  (:use [slingshot.slingshot :only [try+]])
  (:require [authy.core :as authy]
            [cemerick.url :as curl]
            [clj-http.client :as http]
            [mescal.util :as util]
            [clojure.tools.logging :as log]
            [clojure.string :as string]))

(defn- refresh-access-token
  [token-info-fn timeout]
  (let [new-token-info (authy/refresh-access-token @(token-info-fn) :timeout timeout)]
    (dosync (ref-set (token-info-fn) new-token-info))))

(defn- wrap-refresh
  [token-info-fn timeout request-fn]
  (try+
   (request-fn)
   (catch [:status 401] _
     (refresh-access-token token-info-fn timeout)
     (request-fn))))

(defmacro ^:private with-refresh
  [[token-info-fn timeout] & body]
  `(wrap-refresh ~token-info-fn ~timeout #(do ~@body)))

(defn check-access-token
  [token-info-fn timeout]
  (when (authy/token-expiring? @(token-info-fn))
    (refresh-access-token token-info-fn timeout)))

(defn agave-get
  [token-info-fn timeout url]
  (with-refresh [token-info-fn timeout]
    ((comp :result :body)
     (http/get (str url)
               {:oauth-token    (:access-token @(token-info-fn))
                :as             :json
                :conn-timeout   timeout
                :socket-timeout timeout}))))

(defn agave-post
  [token-info-fn timeout url body]
  (with-refresh [token-info-fn timeout]
    ((comp :result :body)
     (http/post (str url)
                {:oauth-token   (:access-token @(token-info-fn))
                 :as            :json
                 :accept        :json
                 :content-type  :json
                 :form-params   body}))))

(defn list-systems
  [base-url token-info-fn timeout]
  (agave-get token-info-fn timeout (curl/url base-url "/systems/v2/")))

(defn get-system-info
  [base-url token-info-fn timeout system-name]
  (agave-get token-info-fn timeout (curl/url base-url "/systems/v2/" system-name)))

(defn list-apps
  [base-url token-info-fn timeout]
  (agave-get token-info-fn timeout (curl/url base-url "/apps/v2/")))

(defn get-app
  [base-url token-info-fn timeout app-id]
  (agave-get token-info-fn timeout (curl/url base-url "/apps/v2" app-id)))

(defn submit-job
  [base-url token-info-fn timeout submission]
  (agave-post token-info-fn timeout (curl/url base-url "/jobs/v2/") submission))

(defn list-jobs
  ([base-url token-info-fn timeout]
     (agave-get token-info-fn timeout (curl/url base-url "/jobs/v2/")))
  ([base-url token-info-fn timeout job-ids]
     (filter (comp (set job-ids) :id) (list-jobs base-url token-info-fn timeout))))

(defn list-job
  [base-url token-info-fn timeout job-id]
  (agave-get token-info-fn timeout (curl/url base-url "/jobs/v2" job-id)))

(def ^:private root-dir-for
  (memoize (fn [base-url token-info-fn timeout storage-system]
             ((comp :rootDir :storage)
              (get-system-info base-url token-info-fn timeout storage-system)))))

(def ^:private get-default-storage-system
  (memoize (fn [base-url token-info-fn timeout]
             (->> (list-systems base-url token-info-fn timeout)
                  (first #(and (= (:type %) "STORAGE") (:default %)))
                  (:id)))))

(defn- get-root-dir
  [base-url token-info-fn timeout storage-system]
  (let [root-dir (root-dir-for base-url token-info-fn timeout storage-system)]
    (util/assert-defined root-dir)
    root-dir))

(defn- get-default-root-dir
  [base-url token-info-fn timeout]
  (get-root-dir base-url token-info-fn timeout
                (get-default-storage-system base-url token-info-fn timeout)))

(defn file-path-to-url
  [url-type base-url token-info-fn timeout storage-system file-path]
  (let [root-dir (get-root-dir base-url token-info-fn timeout storage-system)
        url-path (string/replace file-path (re-pattern (str "\\Q" root-dir "/")) "")]
    (str (curl/url base-url "/files/v2" url-type "system" storage-system url-path))))

(defn- files-base
  [base-url]
  (str (curl/url base-url "/files/v2")))

(defn- files-base-regex
  ([base-url]
     (re-pattern (str "\\Q" (files-base base-url) "\\E/[^/]+")))
  ([base-url system-id]
     (re-pattern (str "\\Q" (files-base base-url) "\\E/[^/]+/system/\\Q" system-id))))

(defn- extract-storage-system
  [base-url file-url]
  (let [regex (re-pattern (str "\\Q" base-url "\\E/files/v2/[^/]+/system/([^/]+)"))]
    (second (re-find regex file-url))))

(defn- build-path
  [base & rest]
  (string/join "/" (concat [(string/replace base #"/$" "")]
                           (map #(string/replace % #"^/|/$" "") rest))))

(defn file-url-to-path
  [base-url token-info-fn timeout file-url]
  (if-let [storage-system (extract-storage-system base-url file-url)]
    (build-path (get-root-dir base-url token-info-fn timeout storage-system)
                (string/replace file-url (files-base-regex base-url storage-system) ""))
    (build-path (get-default-root-dir base-url token-info-fn timeout)
                (string/replace file-url (files-base-regex base-url) ""))))

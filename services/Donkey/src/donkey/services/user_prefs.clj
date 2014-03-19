(ns donkey.services.user-prefs
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes]
        [clj-jargon.init :only [with-jargon]]
        [clj-jargon.item-ops :only [mkdir mkdirs]]
        [clj-jargon.permissions :only [set-owner]]
        [donkey.clients.nibblonian]
        [donkey.util.config]
        [donkey.util.service]
        [donkey.auth.user-attributes])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as cl]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [clj-jargon.item-info :as jinfo]))

(def default-output-dir-key :defaultOutputFolder)

(defn- key-url
  [bucket-fn user]
  (str
    (string/join "/"
      (map ft/rm-last-slash [(riak-base-url) (bucket-fn) user]))
    "?returnbody=true"))

(defn- request-failed
  "Throws an exception for a failed request."
  [resp]
  (throw+ {:error_code ERR_REQUEST_FAILED
           :body       (:body resp)}))

(defn- settings
  ([bucket-fn]
     (let [user (:username current-user)
           url  (key-url bucket-fn user)]
       (log/debug "settings: GET" url)
       (let [resp (cl/get url {:throw-exceptions false})]
         (cond
          (= 200 (:status resp)) (:body resp)
          (= 404 (:status resp)) "{}"
          :else                  (request-failed resp)))))

  ([bucket-fn new-settings]
     (let [user (:username current-user)
           url  (key-url bucket-fn user)]
       (log/debug "settings: POST" url new-settings)
       (let [resp (cl/post
                   url
                   {:content-type :json
                    :body         new-settings
                    :query-params {:dw (riak-prefs-dw)}}
                   {:throw-exceptions false})]
         (cond
          (= 200 (:status resp)) (:body resp)
          :else                  (request-failed resp))))))

(defn- remove-settings
  "Removes saved user settings from the Riak server."
  [bucket-fn]
  (let [user   (:username current-user)
        url    (key-url bucket-fn user)
        _      (log/debug "settings: DELETE" url)
        resp   (cl/delete url {:throw-exceptions false})
        status (:status resp)]
    (cond (= 404 (:status resp))      (success-response)
          (<= 200 (:status resp) 299) (success-response)
          :else                       (request-failed resp))))

(defn- add-default-output-dir
  "Adds the default output directory to a set of user preferences."
  [prefs path]
  (assoc prefs
    default-output-dir-key {:id path :path path}))

(defn- extract-default-output-dir
  "Gets the default output directory from a set of user preferences."
  [prefs]
  (if (map? (default-output-dir-key prefs))
    (:path (default-output-dir-key prefs))
    (default-output-dir-key prefs)))

(defn- system-default-output-dir
  []
  (build-path (home-dir) (default-output-dir)))

(defn- generate-default-output-dir
  "Generates a default output directory for the user and stores it in the preferences."
  [prefs]
  (let [base  (build-path (home-dir) (default-output-dir))
        prefs (add-default-output-dir prefs (gen-output-dir base))]
    (settings riak-prefs-bucket (cheshire/encode prefs))
    prefs))

(defn- add-system-default-output-dir
  "Adds system default output directory to the preferences that are passed in."
  [prefs]
  (if-not (contains? prefs :systemDefaultOutputDir)
    (assoc prefs :systemDefaultOutputDir {:id   (system-default-output-dir)
                                          :path (system-default-output-dir)})
    (if-not (map? (:systemDefaultOutputDir prefs))
      (assoc prefs :systemDefaultOutputDir {:id (:systemDefaultOutputDir prefs)
                                            :path (:systemDefaultOutputDir prefs)})
      prefs)))

(defn- sysdefoutdir
  [prefs]
  (let [out-dir (:systemDefaultOutputDir prefs)]
    (if (map? out-dir)
      (:path out-dir)
      out-dir)))

(defn- create-dir
  [cm user out-dir]
  (log/warn "creating " out-dir)
  (mkdirs cm out-dir)
  (set-owner cm out-dir user))

(defn- create-system-default-output-dir
  "Creates the system default output dir."
  [prefs]
  (let [sys-output-dir (ft/rm-last-slash (sysdefoutdir prefs))
        output-dir     (ft/rm-last-slash (extract-default-output-dir prefs))
        user           (:shortUsername current-user)]
    (log/warn "sys-output-dir" sys-output-dir)
    (log/warn "output-dir" output-dir)
    (with-jargon (jargon-cfg) [cm]
      (cond
       (and (string/blank? output-dir)
            (not (string/blank? sys-output-dir))
            (not (jinfo/exists? cm sys-output-dir)))
       (create-dir cm user sys-output-dir)

       (and (not (string/blank? output-dir))
            (not (jinfo/exists? cm output-dir)))
       (create-dir cm user output-dir)

       :else
       (log/warn "Not creating default output directory for " user)))
    prefs))

(defn handle-blank-default-output-dir
  [prefs]
  (let [output-dir (extract-default-output-dir prefs)]
    (if (string/blank? output-dir)
      (generate-default-output-dir prefs)
      prefs)))

(defn handle-string-default-output-dir
  [prefs]
  (let [output-dir (default-output-dir-key prefs)]
    (if-not (map? output-dir)
      (assoc prefs default-output-dir-key {:id output-dir
                                           :path output-dir})
      prefs)))

(defn- get-user-prefs
  [prefs]
  (log/warn "get-user-prefs")
  (-> prefs
      (handle-blank-default-output-dir)
      (handle-string-default-output-dir)
      (add-system-default-output-dir)
      (create-system-default-output-dir)
      (cheshire/encode)))

(defn- set-user-prefs
  [prefs]
  (->> prefs
    (add-system-default-output-dir)
    (create-system-default-output-dir)
    (cheshire/generate-string)
    (settings riak-prefs-bucket)))

(defn user-prefs
  "Retrieves or saves the user's preferences."
  ([]
     (let [prefs (cheshire/decode (settings riak-prefs-bucket) true)]
       (log/warn "Getting user prefs")
       (get-user-prefs prefs)))
  ([req-prefs-string]
    (let [prefs (cheshire/decode req-prefs-string true)]
      (set-user-prefs prefs))))

(def remove-prefs (partial remove-settings riak-prefs-bucket))
(def search-history (partial settings riak-search-hist-bucket))
(def clear-search-history (partial remove-settings riak-search-hist-bucket))

(defn save-default-output-dir
  "Saves the path to the user's default output folder in the user's preferences."
  [path]
  (-> (user-prefs)
      (cheshire/decode true)
      (add-default-output-dir path)
      (cheshire/encode)
      (user-prefs)))

(defn- create-output-dir
  [user path]
  (with-jargon (jargon-cfg) [cm]
    (when-not (jinfo/exists? cm path)
      (log/warn "Creating output dir")
      (mkdirs cm path)
      (set-owner cm user path))))

(defn get-default-output-dir
  "Gets the path to the user's default output folder from the user's preferences."
  []
  (let [user (:username current-user)
        retv (extract-default-output-dir (cheshire/decode (user-prefs) true))
        dpth (:path retv)]
    retv))

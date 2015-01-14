(ns donkey.services.user-prefs
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes]
        [donkey.clients.user-prefs]
        [donkey.util.service]
        [donkey.auth.user-attributes])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as cl]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [donkey.clients.data-info :as di]
            [donkey.util.config :as cfg]))


(def default-output-dir-key :defaultOutputFolder)

(defn- add-default-output-dir
  "Adds the default output directory to a set of user preferences."
  [prefs path]
  (assoc prefs default-output-dir-key {:id path :path path}))

(defn- extract-default-output-dir
  "Gets the default output directory from a set of user preferences."
  [prefs]
  (if (map? (default-output-dir-key prefs))
    (:path (default-output-dir-key prefs))
    (default-output-dir-key prefs)))

(defn- system-default-output-dir
  []
  (di/build-path (di/user-home-folder (:shortUsername current-user)) (cfg/default-output-dir)))


(defn- generate-default-output-dir
  "Generates a default output directory for the user and stores it in the preferences."
  [user prefs]
  (let [base  (system-default-output-dir)
        prefs (add-default-output-dir prefs (di/gen-output-dir base))]
    (set-prefs user prefs)
    prefs))

(defn- add-system-default-output-dir
  "Adds system default output directory to the preferences that are passed in."
  [prefs]
  (cond
   (not (contains? prefs :systemDefaultOutputDir))
   (assoc prefs :systemDefaultOutputDir {:id   (system-default-output-dir)
                                         :path (system-default-output-dir)})

   (not (map? (:systemDefaultOutputDir prefs)))
   (assoc prefs :systemDefaultOutputDir {:id (:systemDefaultOutputDir prefs)
                                         :path (:systemDefaultOutputDir prefs)})

   :else
   prefs))

(defn- sysdefoutdir
  [prefs]
  (let [out-dir (:systemDefaultOutputDir prefs)]
    (if (map? out-dir)
      (do (println "yay") (println out-dir) (:path out-dir))
      (do (println "nay") out-dir))))


(defn- create-system-default-output-dir
  "Creates the system default output dir."
  [prefs]
  (let [sys-output-dir (ft/rm-last-slash (sysdefoutdir prefs))
        output-dir     (ft/rm-last-slash (extract-default-output-dir prefs))
        user           (:shortUsername current-user)]
    (log/warn "sys-output-dir" sys-output-dir)
    (log/warn "output-dir" output-dir)
    (cond
      (not (string/blank? output-dir))     (di/ensure-dir-created user output-dir)
      (not (string/blank? sys-output-dir)) (di/ensure-dir-created user sys-output-dir)
      :else                                (log/warn "Not creating default output directory for"
                                                     user))
    prefs))

(defn handle-blank-default-output-dir
  [user prefs]
  (let [output-dir (extract-default-output-dir prefs)]
    (if (string/blank? output-dir)
      (generate-default-output-dir user prefs)
      prefs)))

(defn handle-string-default-output-dir
  [prefs]
  (let [output-dir (default-output-dir-key prefs)]
    (if-not (map? output-dir)
      (assoc prefs default-output-dir-key {:id output-dir :path output-dir})
      prefs)))

(defn- get-user-prefs
  [user prefs]
  (log/warn "get-user-prefs")
  (->> prefs
      (handle-blank-default-output-dir user)
      (handle-string-default-output-dir)
      (add-system-default-output-dir)
      (create-system-default-output-dir)))

(defn- set-user-prefs
  [user prefs]
  (->> prefs
    (add-system-default-output-dir)
    (create-system-default-output-dir)
    (set-prefs user)))

(defn validate-user-prefs
  [prefs]
  (if-not (contains? prefs default-output-dir-key)
    (throw+ {:error_code ERR_BAD_REQUEST :msg (str "Missing " (name default-output-dir-key))})))

(defn user-prefs
  "Retrieves or saves the user's preferences."
  ([user]
     (let [prefs (get-prefs user)]
       (log/warn "Getting user prefs")
       (get-user-prefs user prefs)))
  ([user req-prefs-string]
     (let [prefs (if-not (map? req-prefs-string)
                   (cheshire/decode req-prefs-string true)
                   req-prefs-string)]
       (validate-user-prefs prefs)
       (set-user-prefs user prefs))))

(defn remove-prefs
  "Deletes the preferences for the current user."
  []
  (delete-prefs (:username current-user))
  (success-response))

(defn save-default-output-dir
  "Saves the path to the user's default output folder in the user's preferences."
  [user path]
  (user-prefs user (add-default-output-dir (user-prefs user) path)))


(defn get-default-output-dir
  "Gets the path to the user's default output folder from the user's preferences."
  []
  (let [user (:username current-user)
        retv (extract-default-output-dir (cheshire/decode (user-prefs user) true))
        dpth (:path retv)]
    retv))

(defn do-get-prefs
  []
  (success-response (user-prefs (:username current-user))))

(defn do-post-prefs
  [body]
  (success-response (user-prefs (:username current-user) body)))

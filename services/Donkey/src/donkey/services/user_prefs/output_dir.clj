(ns donkey.services.user-prefs.output-dir
  (:use [donkey.auth.user-attributes :only [current-user]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [donkey.clients.data-info :as di]
            [donkey.util.config :as cfg]))

(def default-output-dir-key :defaultOutputFolder)

(defn add-default-output-dir
  "Adds the default output directory to a set of user preferences."
  [prefs path]
  (assoc prefs default-output-dir-key {:id path :path path}))

(defn extract-default-output-dir
  "Gets the default output directory from a set of user preferences."
  [prefs]
  (if (map? (default-output-dir-key prefs))
    (:path (default-output-dir-key prefs))
    (default-output-dir-key prefs)))

(defn- system-default-output-dir
  []
  (di/build-path
   (di/user-home-folder (:shortUsername current-user))
   (cfg/default-output-dir)))

(defn- generate-default-output-dir
  "Generates a default output directory for the user and stores it in the preferences."
  [user prefs]
  (add-default-output-dir prefs (di/gen-output-dir (system-default-output-dir))))

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
      (:path out-dir)
      out-dir)))

(defn- create-system-default-output-dir
  "Creates the system default output dir."
  [prefs]
  (let [sys-output-dir (ft/rm-last-slash (sysdefoutdir prefs))
        output-dir     (ft/rm-last-slash (extract-default-output-dir prefs))
        user           (:shortUsername current-user)]
    (log/debug "sys-output-dir" sys-output-dir)
    (log/debug "output-dir" output-dir)
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

(defn process-outgoing
  [user prefs]
  (->> prefs
       (handle-blank-default-output-dir user)
       (handle-string-default-output-dir)
       (add-system-default-output-dir)
       (create-system-default-output-dir)))

(defn process-incoming
  [user prefs]
  (->> prefs
       (add-system-default-output-dir)
       (create-system-default-output-dir)))
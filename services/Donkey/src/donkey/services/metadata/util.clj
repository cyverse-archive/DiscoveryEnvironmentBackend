(ns donkey.services.metadata.util
  (:require [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.string :as string]
            [clojure-commons.file-utils :as ft]))

(def failed-status "Failed")
(def completed-status "Completed")
(def submitted-status "Submitted")
(def idle-status "Idle")
(def running-status "Running")
(def completed-status-codes #{failed-status completed-status})

(defn- current-timestamp
  []
  (tf/unparse (tf/formatter "yyyy-MM-dd-HH-mm-ss.S") (t/now)))

(defn is-completed?
  [job-status]
  (completed-status-codes job-status))

(def not-completed? (complement is-completed?))

(defn- job-name-to-path
  "Converts a job name to a string suitable for inclusion in a path."
  [path]
  (string/replace path #"[\s@]" "_"))

(defn build-result-folder-path
  [submission]
  (let [build-path (comp ft/rm-last-slash ft/path-join)]
    (if (:create_output_subdir submission true)
      (build-path (:outputDirectory submission)
                  (str (job-name-to-path (:name submission)) "-" (current-timestamp)))
      (build-path (:outputDirectory submission)))))

(defn update-submission-result-folder
  [submission result-folder-path]
  (assoc submission
    :outputDirectory      result-folder-path
    :create_output_subdir false))

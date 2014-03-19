(ns dingle.jenkins
  (:use [dingle.scripting]
        [cemerick.url])
  (:require [cheshire.core :as cheshire]
            [clojure-commons.file-utils :as ft]))

(defn jenkins-job-url
  [jenkins-url job-name jenkins-token]
  (str (-> (url jenkins-url "job" job-name "build")
         (assoc :query {:token jenkins-token}))))

(defn jenkins-lastbuild-url
  [jenkins-url job-name]
  (str (url jenkins-url "job" job-name "lastBuild/api/json")))

(defn jenkins-job-api
  [jenkins-url job-name]
  (str (url jenkins-url "job" job-name "api/json")))

(defn trigger-job
  "Uses curl to do a GET request against a Jenkins URL, triggering a build."
  [jenkins-url job-name jenkins-token]
  (let [full-jurl (jenkins-job-url jenkins-url job-name jenkins-token)]
    (execute
      (scriptify
        (curl ~full-jurl)))))

(defn build-running?
  [jenkins-url job-name]
  (let [full-jurl (jenkins-lastbuild-url jenkins-url job-name)]
    (:building
      (cheshire/decode
        (:out
          (first
            (execute
              (scriptify
               (curl ~full-jurl)))))
        true))))

(defn queued?
  [jenkins-url job-name]
  (let [full-jurl (jenkins-job-api jenkins-url job-name)]
    (:inQueue
      (cheshire/decode
        (:out
          (first
            (execute
              (scriptify
               (curl ~full-jurl)))))
        true))))

(defn building-or-queued?
  [jenkins-url job-name]
  (or (build-running? jenkins-url job-name)
      (queued? jenkins-url job-name)))
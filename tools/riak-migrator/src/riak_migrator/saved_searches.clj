(ns riak-migrator.saved-searches
  (:use [riak-migrator.riak])
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url url-encode]]
            [clojure.string :as string]))

(def non-existant-users (atom #{}))
(def failed-users (atom #{}))

(defn add-non-existant-users
  [username]
  (println "WARNING, USER DOESN'T EXIST: " username)
  (reset! non-existant-users (conj @non-existant-users username)))

(defn add-failed-users
  [username]
  (reset! non-existant-users (conj @failed-users username)))

(defn saved-searches-buckets
  [buckets]
  (filter #(.endsWith % "-reserved") buckets))

(def key-name "queryTemplates")

(defn username-from-bucket
  [bucket]
  (str (string/replace bucket #"\-reserved$" "") "@iplantcollaborative.org"))

(defn saved-searches-url
  [host port]
  (str "http://" host ":" port))

(defn user-exists?
  [surl username]
  (= 200 (:status (http/get (str (url surl (url-encode username)))))))

(defn post-searches
  [svc rb username bucket]
  (let [resp (http/post
              (str (url svc (url-encode username)))
              {:body (key-value rb bucket key-name) :body-encode "UTF-8"
               :content-type "application/json"})]
    (when-not (= 200 (:status resp))
      (println "WARNING, REQUEST FAILED:\n" (:body resp))
      (add-failed-users username))
    (when (= 200 (:status resp))
      (println "\t* -- Successfully migrated data for" username))))

(defn saved-searches
  [options]
  (let [rb  (riak-base (:riak-host options) (:riak-port options))
        svc (saved-searches-url (:service-host options) (:service-port options))]
    (println "Migrating saved searches")
    (println "\t* -- Riak host: " rb)
    (println "\t* -- Service host: " svc)

    (doseq [bucket (saved-searches-buckets (buckets rb))]
      (let [username (username-from-bucket bucket)]
        (if-not (user-exists? svc username)
          (add-non-existant-users username)
          (post-searches svc rb username bucket))))))

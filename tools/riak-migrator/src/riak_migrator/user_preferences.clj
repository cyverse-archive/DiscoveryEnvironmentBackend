(ns riak-migrator.user-preferences
  (:use [riak-migrator.db]
        [riak-migrator.riak]
        [kameleon.user-prefs-queries])
  (:require [clj-http.client :as http]
            [cemerick.url :refer (url url-encode)]))

(def prefs-failed-users (atom #{}))

(defn add-failed-prefs-users
  [username]
  (reset! prefs-failed-users (conj @prefs-failed-users username)))

(defn prefs-from-riak
  [rb bucket username]
  (println "\t* -- GET" (str (url rb "riak" bucket (url-encode username))))
  (let [rsp (http/get (str (url rb "riak" bucket (url-encode username))) {:throw-exceptions false})]
    (if-not (<= 200 (:status rsp) 299)
      (do (add-failed-prefs-users username)
        (println "\t* -- Preferences not found for user" username "; status=" (:status rsp)))
      (:body rsp))))

(defn store-user-prefs
  [svc username prefs]
  (let [svc-url (str (url svc username))
        _       (println "\t* -- POST" svc-url)
        rsp     (http/post svc-url {:body prefs :content-type :json})
        status  (:status rsp)]
    (when-not (<= 200 status 299)
      (println "\t* -- Failed to store preferences for" username "; status=" status)
      (add-failed-prefs-users username))
    (when (<= 200 status 299)
      (println "\t* -- Stored preferences for" username))))

(defn user-preferences
  [options]
  (connect-db options)
  (let [rb  (riak-base (:riak-host options) (:riak-port options))
        bk  (:riak-bucket options)
        svc (str "http://" (:service-host options) ":" (:service-port options))]
    (println "Migrating saved searches")
    (println "\t* -- Riak host: " rb)

    (doseq [username (all-users)]
      (when-let [prefs (prefs-from-riak rb bk username)]
        (store-user-prefs svc username prefs)
        (println "")))

    (println "Session migration failed for the following users:")
    (doseq [user (seq @prefs-failed-users)]
      (println "*" user))))

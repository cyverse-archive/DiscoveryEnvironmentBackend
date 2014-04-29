(ns riak-migrator.user-sessions
  (:use [riak-migrator.db]
        [riak-migrator.riak]
        [kameleon.user-session-queries])
  (:require [clj-http.client :as http]
            [cemerick.url :refer (url url-encode)]))

(def session-failed-users (atom #{}))

(defn add-failed-session-users
  [username]
  (reset! session-failed-users (conj @session-failed-users username)))

(defn session-from-riak
  [rb username]
  (println "\t* -- Attempting GET" (str (url rb "riak" "sessions" (url-encode username))))
  (let [rsp (http/get (str (url rb "riak" "sessions" (url-encode username))) {:throw-exceptions false})]
    (if-not (<= 200 (:status rsp) 299)
      (do (add-failed-session-users username)
        (println "\t* -- Session not found for user" username "; status=" (:status rsp)))
      (:body rsp))))

(defn store-user-session
  [svc username session]
  (let [svc-url (str (url svc username))
        _       (println "\t* -- POST" svc-url)
        rsp     (http/post svc-url {:body session :content-type :json})
        status  (:status rsp)]
    (when-not (<= 200 status 299)
      (println "\t* -- Failed to store session for" username "; status=" status)
      (add-failed-session-users username))
    (when (<= 200 status 299)
      (println "\t* -- Stored session for" username))))

(defn user-sessions
  [options]
  (connect-db options)
  (let [rb  (riak-base (:riak-host options) (:riak-port options))
        svc (str "http://" (:service-host options) ":" (:service-port options))]
    (println "Migrating saved searches")
    (println "\t* -- Riak host: " rb)

    (doseq [username (all-users)]
      (when-let [session (session-from-riak rb username)]
        (store-user-session svc username session)
        #_(save-user-session username session)))

    (println "Session migration failed for the following users:")
    (doseq [user (seq @session-failed-users)]
      (println "*" user))))

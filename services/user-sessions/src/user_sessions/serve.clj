(ns user-sessions.serve
  (:use [kameleon.user-session-queries]
        [kameleon.misc-queries]
        [compojure.response]
        [ring.util.response]
        [medley.core])
  (:require [cheshire.core :as json]))

(defn sanitize
  [session-str]
  (if session-str
    (filter-keys #(not= :id %) (json/parse-string session-str))
    {}))

(defn not-a-user
  [username]
  {:status 404 :body {:user username}})

(defn get-req
  [username req]
  (if-not (user? username)
    (not-a-user username)
    (response (sanitize (user-session username)))))

(defn post-req
  [username req]
  (if-not (user? username)
    (not-a-user username)
    (response (sanitize (save-user-session username (json/encode (:body req)))))))

(defn delete-req
  [username]
  (if-not (user? username)
    (not-a-user username)
    (response (sanitize (delete-user-session username)))))

(defn put-req
  [username req]
  (post-req username req))

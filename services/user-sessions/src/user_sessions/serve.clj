(ns user-sessions.serve
  (:use [kameleon.user-session-queries]
        [kameleon.misc-queries]
        [compojure.response]
        [ring.util.response])
  (:require [cheshire.core :as json]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(defn get-req
  [username req]
  (info req)
  (if-not (user? username)
    (not-found {:user username})
    (response (user-session username))))

(defn post-req
  [username req]
  (info req)
  (if-not (user? username)
    {:status 404 :body {:user username}}
    (response (save-user-session username (json/encode (:body req))))))

(defn delete-req
  [username]
  (info "delete" username)
  (if-not (user? username)
    {:status 404 :body {:user username}})
    (response (delete-user-session username)))

(defn put-req
  [username req]
  (info req)
  (post-req username req))

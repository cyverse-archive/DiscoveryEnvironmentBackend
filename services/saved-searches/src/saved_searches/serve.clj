(ns saved-searches.serve
  (:use [kameleon.user-saved-searches-queries]
        [kameleon.misc-queries]
        [compojure.response]
        [ring.util.response]
        [medley.core])
  (:require [cheshire.core :as json]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(defn not-a-user
  [username]
  {:status 404 :body {:user username}})

(defn invalid-content
  [content-type]
  {:status 415 :body {:content-type content-type}})

(defmacro validate
  [[username req content-type?] & body]
  `(cond
    (not (user? ~username))
    (not-a-user ~username)

    (and ~content-type? (not= (:content-type ~req) "application/json"))
    (invalid-content (:content-type ~req))

    :else
    (do ~@body)))

(defn get-req
  [username req]
  (validate
   [username req false]
   (response (json/parse-string (saved-searches username) true))))

(defn post-req
  [username req]
  (validate
   [username req true]
   (response (save-saved-searches username (json/encode (:body req))))))

(defn delete-req
  [username req]
  (validate
   [username req false]
   (response (delete-saved-searches username))))

(defn put-req
  [username req]
  (post-req username req))

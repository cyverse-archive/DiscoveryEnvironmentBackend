(ns saved-searches.serve
  (:use [kameleon.user-saved-searches-queries]
        [kameleon.misc-queries]
        [compojure.response]
        [ring.util.response]
        [medley.core])
  (:require [cheshire.core :as json]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(defn parse-saved-searches
  [saved-searches]
  (if (contains? saved-searches :saved_searches)
    (let [parsed (json/parse-string (:saved_searches saved-searches) true)]
      (assoc saved-searches :saved_searches parsed))
    saved-searches))

(defn sanitize
  [saved-searches]
  (if saved-searches
    (-> saved-searches
        (dissoc :user_id :id)
        (parse-saved-searches))))

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
   (response (saved-searches username))))

(defn post-req
  [username req]
  (validate
   [username req true]
   (response (sanitize (save-saved-searches username (json/encode (:body req)))))))

(defn delete-req
  [username req]
  (if-not (user? username)
    ""
    (do
      (delete-saved-searches username)
      "")))

(defn put-req
  [username req]
  (post-req username req))

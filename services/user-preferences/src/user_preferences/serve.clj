(ns user-preferences.serve
  (:use [kameleon.user-prefs-queries]
        [kameleon.misc-queries]
        [compojure.response]
        [ring.util.response]
        [medley.core])
  (:require [cheshire.core :as json]))

(defn sanitize
  [prefs]
  (if prefs
    (let [prefs? (contains? prefs :preferences)
          filtered  (filter-keys #(not (contains? #{:id :user_id} %)) prefs)]
      (if prefs?
        (-> filtered
            (dissoc :preferences)
            (assoc :preferences (json/parse-string (:preferences prefs) true)))
        filtered))
    {}))

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
   (response (sanitize (json/parse-string (user-prefs username) true)))))

(defn post-req
  [username req]
  (validate
   [username req true]
   (response (sanitize (save-user-prefs username (json/encode (:body req)))))))

(defn delete-req
  [username req]
  (if-not (user? username)
    ""
    (do
      (delete-user-prefs username)
      "")))

(defn put-req
  [username req]
  (post-req username req))

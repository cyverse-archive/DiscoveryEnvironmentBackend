(ns saved-searches.serve
  (:use [kameleon.user-saved-searches-queries]
        [kameleon.misc-queries]
        [compojure.response]
        [ring.util.response]
        [medley.core])
  (:require [cheshire.core :as json]))

(defn sanitize
  [prefs]
  (if prefs
    (let [prefs? (contains? prefs :saved_searches)
          filtered  (filter-keys #(not (contains? #{:id :user_id} %)) prefs)]
      (if prefs?
        (-> filtered
            (dissoc :saved_searches)
            (assoc :saved_searches (json/parse-string (:saved_searches prefs) true)))
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
   (response (sanitize (json/parse-string (saved-searches username) true)))))

(defn post-req
  [username req]
  (validate
   [username req true]
   (response (sanitize (save-saved-searches username (json/encode (:body req)))))))

(defn delete-req
  [username req]
  (validate
   [username req false]
   (response (sanitize (delete-saved-searches username)))))

(defn put-req
  [username req]
  (post-req username req))

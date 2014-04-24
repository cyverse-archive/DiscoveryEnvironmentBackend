(ns user-sessions.serve
  (:use [kameleon.user-session-queries]
        [kameleon.misc-queries]
        [compojure.response]
        [ring.util.response]
        [medley.core])
  (:require [cheshire.core :as json]))

(defn sanitize
  [session]
  (if session
    (let [session? (contains? session :session)
          filtered  (filter-keys #(not (contains? #{:id :user_id} %)) session)]
      (if session?
        (-> filtered
            (dissoc :session)
            (assoc :session (json/parse-string (:session session))))
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
   (if-not (user? username)
     (not-a-user username)
     (response (sanitize (json/parse-string (user-session username)))))))

(defn post-req
  [username req]
  (validate
   [username req true]
   (response (sanitize (save-user-session username (json/encode (:body req)))))))

(defn delete-req
  [username req]
  (validate
   [username req false]
   (response (sanitize (delete-user-session username)))))

(defn put-req
  [username req]
  (post-req username req))

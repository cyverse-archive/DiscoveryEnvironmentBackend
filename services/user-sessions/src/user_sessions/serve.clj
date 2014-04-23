(ns user-sessions.serve
  (:use [kameleon.user-session-queries]
        [kameleon.misc-queries]
        [compojure.response]
        [ring.util.response]))

(defn head-req
  [username req]
  {:msg "lol"})

(defn get-req
  [username req]
  (if-not (user? username)
    (not-found {:user username})
    (response (user-session username))))

(defn post-req
  [username req]
  {:msg "post lol"})

(defn delete-req
  [username]
  {:msg "delete lol"})

(defn put-req
  [username req]
  {:msg "put lol"})

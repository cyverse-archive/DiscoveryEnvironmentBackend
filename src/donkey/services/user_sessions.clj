(ns donkey.services.user-sessions
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes]
        [donkey.util.config]
        [donkey.util.service]
        [donkey.auth.user-attributes])
  (:require [clj-http.client :as cl]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]))

(defn- key-url
  [user]
  (str 
    (string/join "/" 
      (map ft/rm-last-slash [(riak-base-url) (riak-sessions-bucket) user]))
    "?returnbody=true"))

(defn- request-failed
  "Throws an exception for a failed request."
  [resp]
  (throw+ {:error_code ERR_REQUEST_FAILED
           :body       (:body resp)}))

(defn user-session
  ([]
    (let [user (:username current-user)]
      (log/debug (str "user-session: GET " (key-url user)))
      (let [resp (cl/get (key-url user) {:throw-exceptions false})]
        (cond
          (= 200 (:status resp)) (:body resp)
          (= 404 (:status resp)) "{}"
          :else                  (request-failed resp)))))

  ([new-session]
    (let [user (:username current-user)]
      (log/debug (str "user-session: POST " (key-url user) " " new-session))
      (let [resp (cl/post 
                   (key-url user) 
                   {:content-type :json :body new-session} 
                   {:throw-exceptions false})]
        (cond
          (= 200 (:status resp)) (:body resp)
          :else                  (request-failed resp))))))

(defn remove-session
  "Removes user session information from the Riak cluster."
  []
  (let [user   (:username current-user)
        url    (key-url user)
        _      (log/debug "user-session: DELETE" url)
        resp   (cl/delete url {:throw-exceptions false})
        status (:status resp)]
    (cond (= 404 (:status resp))      (success-response)
          (<= 200 (:status resp) 299) (success-response)
          :else                       (request-failed resp))))

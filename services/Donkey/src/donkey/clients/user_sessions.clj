(ns donkey.clients.user-sessions
  (:use [donkey.util.config]
        [clojure-commons.error-codes]
        [slingshot.slingshot :only [throw+]])
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [cheshire.core :as json]))

(defn session-url
  [user]
  (str (url (sessions-base) user)))

(defn get-session
  [user]
  (let [resp (http/get (session-url user))]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ERR_NOT_A_USER :user user})

     (= (:status resp) 400)
     (throw+ {:error_code ERR_BAD_REQUEST :user user})

     (= (:status resp) 500)
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Error thrown by user-sessions service"})

     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the user-sessions service"})

     :else
     (json/parse-string (:body resp) true))))

(defn set-session
  [user session]
  (let [req-options  {:body session
                      :content-type "application/json"
                      :throw-exceptions false}
        resp         (http/post (session-url user) req-options)]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ERR_NOT_A_USER :user user})

     (= (:status resp) 400)
     (throw+ {:error_code ERR_BAD_REQUEST :session session})

     (= (:status resp) 415)
     (throw+ {:error_code ERR_BAD_REQUEST :content-type "application/json"})

     (= (:status resp) 500)
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Error thrown by user-sessions service"})

     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the user-sessions service"})

     :else
     (json/parse-string (:body resp) true))))

(defn delete-session
  [user]
  (let [resp (http/delete (session-url user))]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ERR_NOT_A_USER :user user})
     
     (= (:status resp) 400)
     (throw+ {:error_code ERR_BAD_REQUEST :user user})
     
     (= (:status resp) 500)
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Error thrown by user-sessions service"})
     
     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the user-sessions service"}))))

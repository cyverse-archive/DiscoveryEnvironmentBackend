(ns donkey.clients.saved-searches
  (:use [donkey.util.config]
        [donkey.util.service :only [success-response]]
        [clojure-commons.error-codes]
        [slingshot.slingshot :only [throw+]])
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [cheshire.core :as json]))

(defn saved-searches-url
  [user]
  (str (url (saved-searches-base) user)))

(defn get-saved-searches
  [user]
  (let [resp (http/get (saved-searches-url user))]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ERR_NOT_A_USER :user user})

     (= (:status resp) 400)
     (throw+ {:error_code ERR_BAD_REQUEST :user user})

     (= (:status resp) 500)
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Error thrown by the saved-searches service"})

     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the saved-searches service"})

     :else
     (success-response (:body resp)))))

(defn set-saved-searches
  [user session]
  (let [req-options  {:body session
                      :content-type "application/json"
                      :throw-exceptions false}
        resp         (http/post (saved-searches-url user) req-options)]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ERR_NOT_A_USER :user user})

     (= (:status resp) 400)
     (throw+ {:error_code ERR_BAD_REQUEST :session session})

     (= (:status resp) 415)
     (throw+ {:error_code ERR_BAD_REQUEST :content-type "application/json"})

     (= (:status resp) 500)
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Error thrown by the saved-searches service"})

     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the saved-searches service"})

     :else
     (success-response (:body resp)))))

(defn delete-saved-searches
  [user]
  (let [resp (http/delete (saved-searches-url user))]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ERR_NOT_A_USER :user user})
     
     (= (:status resp) 400)
     (throw+ {:error_code ERR_BAD_REQUEST :user user})
     
     (= (:status resp) 500)
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Error thrown by the saved-searches service"})
     
     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the saved-searches service"})

     :else
     (success-response))))

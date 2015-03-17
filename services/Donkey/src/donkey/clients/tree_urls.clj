(ns donkey.clients.tree-urls
  (:use [donkey.util.config]
        [clojure-commons.error-codes]
        [slingshot.slingshot :only [throw+]])
  (:require [clj-http.client :as http]
            [cemerick.url :refer [url]]
            [cheshire.core :as json]))

(defn tree-urls-url
  [sha1]
  (str (url (tree-urls-base) sha1)))

(defn get-tree-urls
  [sha1]
  (let [resp (http/get (tree-urls-url sha1) {:throw-exceptions false})]
    (cond
     (= (:status resp) 404)
     nil

     (= (:status resp) 400)
     (throw+ {:error_code ERR_BAD_REQUEST :sha1 sha1})

     (= (:status resp) 500)
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Error thrown by tree-urls service"})

     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the tree-urls service"})

     :else
     (json/parse-string (:body resp) true))))

(defn set-tree-urls
  [sha1 tree-urls]
  (let [req-options  {:body (json/generate-string tree-urls)
                      :content-type "application/json"
                      :throw-exceptions false}
        resp         (http/post (tree-urls-url sha1) req-options)]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ERR_DOES_NOT_EXIST :sha1 sha1})

     (= (:status resp) 400)
     (throw+ {:error_code ERR_BAD_REQUEST :sha1 sha1 :tree-urls tree-urls})

     (= (:status resp) 415)
     (throw+ {:error_code ERR_BAD_REQUEST :content-type "application/json"})

     (= (:status resp) 500)
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Error thrown by tree-urls service"})

     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the tree-urls service"})

     :else
     (json/parse-string (:body resp) true))))

(defn delete-tree-urls
  [sha1]
  (let [resp (http/delete (tree-urls-url sha1))]
    (cond
     (= (:status resp) 404)
     (throw+ {:error_code ERR_DOES_NOT_EXIST :sha1 sha1})
     
     (= (:status resp) 400)
     (throw+ {:error_code ERR_BAD_REQUEST :sha1 sha1})
     
     (= (:status resp) 500)
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Error thrown by tree-urls service"})
     
     (not (<= 200 (:status resp) 299))
     (throw+ {:error_code ERR_UNCHECKED_EXCEPTION :msg "Unknown error thrown by the tree-urls service"}))))

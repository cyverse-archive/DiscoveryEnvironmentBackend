(ns donkey.util.validators
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes])
  (:require [cheshire.core :as json]
            [cemerick.url :as url-parser]))

(defn parse-body
  [body]
  (try+
   (json/parse-string body true)
   (catch Exception e
     (throw+ {:error_code ERR_INVALID_JSON
              :message    (str e)}))))

(defn parse-url
  [url-str]
  (try+
   (url-parser/url url-str)
   (catch java.net.UnknownHostException e
     (throw+ {:error_code ERR_INVALID_URL
              :url url-str}))
   (catch java.net.MalformedURLException e
     (throw+ {:error_code ERR_INVALID_URL
              :url url-str}))))

(defn validate-param
  [param-name param-value]
  (when (nil? param-value)
    (let [param-name (if (keyword? param-name) (name param-name) param-name)]
      (throw+ {:error_code ERR_BAD_REQUEST
               :reason     (str "missing request parameter: " param-name)}))))

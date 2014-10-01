(ns data-info.util.validators
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes])
  (:require [cheshire.core :as json]
            [cemerick.url :as url-parser])
  (:import [java.util UUID]))


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


(defn extract-uri-uuid
  "Converts a UUID from text taken from a URI. If the text isn't a UUID, it throws an exception.

   Parameters:
     uuid-txt - The URI text containing a UUID.

   Returns:
     It returns the UUID.

   Throws:
     It throws an ERR_NOT_FOUND if the text isn't a UUID."
  [uuid-txt]
  (try+
    (UUID/fromString uuid-txt)
    (catch IllegalArgumentException _ (throw+ {:error_code ERR_NOT_FOUND}))))

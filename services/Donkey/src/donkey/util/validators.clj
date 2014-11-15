(ns donkey.util.validators
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes])
  (:require [clojure.set :as set]
            [clojure.string :as string]
            [cheshire.core :as json]
            [cemerick.url :as url-parser]
            [donkey.util.config :as cfg])
  (:import [clojure.lang Keyword]
           [java.util UUID]))

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


(defn ^Boolean good-string?
  "Checks that a string doesn't contain any problematic characters.

   Params:
     to-check - The string to check

   Returns:
     It returns false if the string contains at least one problematic character, otherwise false."
  [^String to-check]
  (let [bad-chars      (set (seq (cfg/fs-bad-chars)))
        chars-to-check (set (seq to-check))]
    (empty? (set/intersection bad-chars chars-to-check))))


(defn ^Keyword resolve-entity-type
  "Resolves an entity type keyword value from a URL query parameter.

   Params:
     param-val - The value of the entity-type URL parameter

   Returns:
     The resolved Keyword value.

   Throws:
     :invalid-argument - This is thrown if the extracted type isn't valid."
  [^String param-val]
  (if (empty? param-val)
    :any
    (case (string/lower-case param-val)
      "any"    :any
      "file"   :file
      "folder" :folder
      (throw+ {:type   :invalid-argument
               :reason "must be 'any', 'file' or 'folder'"
               :arg    "entity-type"
               :val    param-val}))))

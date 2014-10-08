(ns data-info.util.validators
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as json]
            [clojure-commons.error-codes :as error])
  (:import [java.util UUID]))


(defn parse-body
  [body]
  (try+
   (json/parse-string body true)
   (catch Exception e
     (throw+ {:error_code error/ERR_INVALID_JSON
              :message    (str e)}))))


(def ^:private uuid-regexes
  [#"^\p{XDigit}{8}(?:-\p{XDigit}{4}){3}-\p{XDigit}{12}$"
   #"^[at]\p{XDigit}{32}"])


(defn- is-uuid?
  [id]
  (some #(re-find % id) uuid-regexes))


(defn valid-uuid-param
  "Validates that a given value is a UUID.

   Parameters:
     param-name - the name of the param holding the proposed UUID
     param-val  - the proposed UUID

   Throws:
     It throws a map with of the following form.

       {:error_code ERR_BAD_REQUEST
        :param      param-name
        :value      param-val}"
  [^String param-name ^String param-val]
  (when-not (is-uuid? param-val)
    (throw+ {:error_code error/ERR_BAD_REQUEST
             :param      param-name
             :value      param-val})))


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
    (catch IllegalArgumentException _
      (throw+ {:error_code error/ERR_NOT_FOUND}))))

(ns data-info.util.validators
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure-commons.error-codes :as error]))


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

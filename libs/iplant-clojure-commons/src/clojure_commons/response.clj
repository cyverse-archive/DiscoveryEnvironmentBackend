(ns clojure-commons.response
  (:require [cheshire.core :as cheshire]
            [clojure-commons.error-codes :as ce]
            [ring.util.response :as resp]
            [ring.util.http-response :as http-resp]))

(defn error-response
  "Builds an HTTP response with a standrd JSON error response body."
  [resp-fn error-code & {:as kvs}]
  (let [body (cheshire/encode (assoc kvs :error_code error-code))]
    (resp-fn body)))

(defn unauthorized
  "Returns an HTTP response indicating that the user is not authorized."
  [reason]
  (-> (error-response http-resp/unauthorized ce/ERR_NOT_AUTHORIZED :reason reason)
      (resp/header "WWW-Authenticate" "Custom")))

(defn forbidden
  "Returns an HTTP response indicating that the user is not permitted to access a resource."
  [reason]
  (error-response http-resp/forbidden ce/ERR_FORBIDDEN :reason reason))

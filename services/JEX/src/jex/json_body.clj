(ns jex.json-body
  (:use [clojure.java.io :only [reader]]
        [clojure-commons.error-codes]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]))

(defn- json-body?
  [request]
  (let [content-type (:content-type request)]
    (not (empty? (re-find #"^application/json" content-type)))))

(defn get-json
  [request]
  (try
    (cheshire/decode-stream (reader (:body request)) true)
    (catch Exception e
            (throw+ {:error_code ERR_INVALID_JSON}))))

(defn- valid-method?
  [request]
  (cond
    (= (:request-method request) "post") true
    (= (:request-method request) :post)  true
    (= (:request-method request) "put")  true
    (= (:request-method request) :put)   true
    :else false))

(defn parse-json-body [handler]
  (fn [request]
    (cond
      (not (valid-method? request))
      (handler request)

      (not (contains? request :body))
      (handler request)

      (not (json-body? request))
      (handler request)

      :else
      (try+
        (let [body-map (get-json request)
              new-req  (assoc request :body body-map)]
          (handler new-req))
        (catch error? err
          (log/error (format-exception (:throwable &throw-context)))
          (err-resp "parse-json" (:object &throw-context)))
        (catch java.lang.Exception e
          (log/error (format-exception (:throwable &throw-context)))
          (err-resp "parse-json" (unchecked &throw-context)))))))

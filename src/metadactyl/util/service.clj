(ns metadactyl.util.service
  (:use [clojure.java.io :only [reader]]
        [clojure.string :only [join upper-case]]
        [ring.util.response :only [charset]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]))

(defn empty-response []
  {:status 200})

(defn success-response
  ([map]
     (charset
      {:status       200
       :body         (cheshire/encode (merge {:success true} map))
       :content-type :json}
      "UTF-8"))
  ([]
     (success-response {})))

(defn failure-response [e]
  (log/error e "bad request")
  (charset
   {:status       400
    :body         (cheshire/encode {:success false :reason (.getMessage e)})
    :content-type :json}
   "UTF-8"))

(defn slingshot-failure-response [m]
  (log/error "bad request:" m)
  (charset
   {:status       400
    :body         (cheshire/encode (assoc (dissoc m :type)
                                     :code    (upper-case (name (or (:type m) (:code m))))
                                     :success false))
    :content-type :json}
   "UTF-8"))

(defn forbidden-response [e]
  (log/error e "unauthorized")
  {:status 401})

(defn error-response [e]
  (log/error e "internal error")
  (charset
   {:status 500
    :body (cheshire/encode {:success false :reason (.getMessage e)})
    :content-type :json}
   "UTF-8"))

(defn unrecognized-path-response []
  "Builds the response to send for an unrecognized service path."
  (let [msg "unrecognized service path"]
    (cheshire/encode {:success false :reason msg})))

(defn trap
  "Traps any exception thrown by a service and returns an appropriate
   repsonse."
  [f]
  (try+
    (f)
    (catch [:type ::unauthorized] {:keys [user message]}
      (log/error message user)
      (forbidden-response (:throwable &throw-context)))
    (catch map? m (slingshot-failure-response m))
    (catch IllegalArgumentException e (failure-response e))
    (catch IllegalStateException e (failure-response e))
    (catch Throwable t (error-response t))))

(defn build-url
  "Builds a URL from a base URL and one or more URL components."
  [base & components]
  (join "/" (map #(.replaceAll % "^/|/$" "")
                 (cons base components))))

(defn prepare-forwarded-request
  "Prepares a request to be forwarded to a remote service."
  [request body]
  {:content-type (get-in request [:headers :content-type])
   :headers (dissoc (:headers request) "content-length" "content-type")
   :body body})

(defn parse-json
  "Parses a JSON request body."
  [body]
  (try+
    (if (string? body)
      (cheshire/decode body true)
      (cheshire/decode-stream (reader body) true))
    (catch Exception e
      (throw+ {:error_code ce/ERR_INVALID_JSON
               :detail     (str e)}))))

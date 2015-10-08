(ns notification-agent.common
  (:use [clojure.java.io :only [reader]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-time.core :as time])
  (:import [java.io InputStream Reader]
           [java.util UUID]))

(defn parse-body
  "Parses a JSON request body, throwing an IllegalArgumentException if the
   body can't be parsed."
  [body]
  (try+
    (if (or (instance? InputStream body) (instance? Reader body))
      (cheshire/decode-stream (reader body) true)
      (cheshire/decode body true))
    (catch Throwable t
      (throw+ {:type    :clojure-commons.exception/invalid-json
               :details (.getMessage t)}))))

(defn validate-user
  "Validates the username that was passed in. Returns the username when valid."
  [user]
  (when (nil? user)
    (throw+ {:type  :clojure-commons.exception/illegal-argument
             :param :user}))
  user)



(defn valid-email-addr
  "Validates an e-mail address."
  [addr]
  (and (not (nil? addr)) (re-matches #"^[^@ ]+@[^@ ]+$" addr)))

(defn string->long
  "Converts a string to a long integer."
  [s details exception-info-map]
  (try+
   (Long/parseLong s)
   (catch NumberFormatException e
     (throw+
      (merge {:type    :clojure-commons.exception/illegal-argument
              :details details}
             exception-info-map)))))

(defn millis-since-epoch [] (str (time/in-millis (time/interval (time/epoch) (time/now)))))

(defn parse-uuid
  "Parses a UUID in the standard format."
  [uuid]
  (and uuid
       (try+
        (UUID/fromString uuid)
        (catch IllegalArgumentException _
          (throw+ {:type        :clojure-commons.exception/bad-request-field
                   :description "invalid UUID"
                   :value       uuid})))))

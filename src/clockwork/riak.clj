(ns clockwork.riak
  (:use [cheshire.core :only [parse-string]]
        [clj-time.format :only [parse formatter]]
        [clockwork.config :only [riak-base]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cemerick.url :as curl]
            [clojure-commons.client :as client]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.riak :as cr]
            [clojure.tools.logging :as log]))

(def ^:private fmt
  "The formatter to use when parsing timestamps."
  (formatter "EEE, dd MMM YYYY HH:mm:ss 'GMT'"))

(defn object-url
  "Builds a Riak URL that refers to an object."
  [bucket k]
  (curl/url (riak-base) bucket k))

(defn list-keys
  "Lists the keys in a Riak bucket."
  [bucket]
  (cr/list-keys (riak-base) bucket))

(defn- parse-last-modified-date
  [date-str]
  (try+
   (parse fmt date-str)
   (catch Exception e
     (log/warn "unable to parse last modified date:" date-str))))

(defn object-last-modified
  "Gets the last modified timestamp of an object."
  [bucket k]
  (let [res (cr/get-object (riak-base) bucket k {:throw-exceptions false})]
    (if (<= 200 (:status res) 299)
      (parse-last-modified-date (get-in res [:headers "last-modified"]))
      (log/warn "unable to find last modified date of:" bucket "-" k))))

(defn remove-object
  "Removes an object from Riak."
  [bucket k]
  (cr/delete-object (riak-base) bucket k))

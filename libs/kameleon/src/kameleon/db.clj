(ns kameleon.db
  (:use [korma.core :exclude [update]]
        [korma.db])
  (:require [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.string :as string])
  (:import [java.sql Timestamp]))

(defn ->enum-val
  [val]
  (raw (str \' val \')))

(def ^:private timestamp-parser
  (tf/formatter (t/default-time-zone)
    "EEE MMM dd YYYY HH:mm:ss 'GMT'Z"
    "YYYY MMM dd HH:mm:ss"
    "YYYY-MM-dd-HH-mm-ss.SSS"
    "YYYY-MM-dd HH:mm:ss.SSS"
    "YYYY-MM-dd'T'HH:mm:ss.SSSZ"))

(defn- strip-time-zone
  "Removes the time zone abbreviation from a date timestamp."
  [s]
  (string/replace s #"\s*\(\w+\)\s*$" ""))

(defn- parse-timestamp
  "Parses a timestamp in one of the accepted formats, returning the number of milliseconds
   since the epoch."
  [s]
  (.getMillis (tf/parse timestamp-parser (strip-time-zone s))))

(defn millis-from-str
  "Parses a string representation of a timestamp."
  [s]
  (assert (or (nil? s) (string? s)))
  (cond (or (string/blank? s) (= "0" s)) nil
    (re-matches #"\d+" s)            (Long/parseLong s)
    :else                            (parse-timestamp s)))

(defn timestamp-from-millis
  "Converts the number of milliseconds since the epoch to a timestamp."
  [millis]
  (when-not (nil? millis)
    (Timestamp. millis)))

(defn timestamp-from-str
  "Parses a string representation of a timestamp."
  [s]
  (timestamp-from-millis (millis-from-str s)))

(defn millis-from-timestamp
  "Converts a timestamp to the number of milliseconds since the epoch."
  [timestamp]
  (when-not (nil? timestamp)
    (.getTime timestamp)))

(defn now
  "Returns a timestamp representing the current date and time."
  []
  (Timestamp. (System/currentTimeMillis)))

(defn timestamp-str
  "Returns a string containing the number of milliseconds since the epoch for a timestamp."
  [timestamp]
  (when-not (nil? timestamp)
    (str (.getTime timestamp))))

(defn now-str
  "Returns a string containing the current number of milliseconds since the epoch."
  []
  (str (System/currentTimeMillis)))

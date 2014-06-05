(ns donkey.util.time
  (:use [clj-time.core :only [default-time-zone]]
        [clj-time.format :only [formatter formatters parse]])
  (:require [clojure.string :as string]))

(def ^:private timestamp-parser
  (formatter (default-time-zone)
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
  (.getMillis (parse timestamp-parser (strip-time-zone s))))

(defn millis-from-str
  "Parses a string representation of a timestamp."
  [s]
  (assert (or (nil? s) (string? s)))
  (cond (or (string/blank? s) (= "0" s)) nil
        (re-matches #"\d+" s)            (Long/parseLong s)
        :else                            (parse-timestamp s)))

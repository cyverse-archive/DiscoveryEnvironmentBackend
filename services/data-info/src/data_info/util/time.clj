(ns data-info.util.time
  (:require [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.string :as string]))


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

(defn format-timestamp
  "Formats a timestamp in a standard format."
  [timestamp]
  (if-not (or (string/blank? timestamp) (= "0" timestamp))
    (tf/unparse (:date-time tf/formatters) (tc/from-long (Long/parseLong timestamp)))
    ""))

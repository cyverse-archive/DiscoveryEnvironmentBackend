(ns notification-agent.time
 (:use [clj-time.core :only (default-time-zone now)]
       [clj-time.format :only (formatter parse unparse)]
       [notification-agent.common :only [string->long]])
 (:require [clojure.string :as string])
 (:import [org.joda.time DateTime]))

(def accepted-timestamp-formats
  ^{:private true
    :doc "The formats that we support for incoming timestamps"}
  ["EEE MMM dd YYYY HH:mm:ss 'GMT'Z"
   "YYYY MMM dd HH:mm:ss"
   "YYYY-MM-dd-HH-mm-ss.SSS"
   "YYYY-MM-dd HH:mm:ss.SSS"
   "YYYY-MM-dd'T'HH:mm:ss.SSSZ"])

(def date-formatter
  ^{:private true
    :doc "The date formatter that is used to format all timestamps."}
  (formatter "EEE MMM dd YYYY HH:mm:ss 'GMT'Z (z)" (default-time-zone)))

(def date-parser
  ^{:private true
    :doc "The date formatter that is used to parse all timestamps."}
  (apply formatter (default-time-zone) accepted-timestamp-formats))

(defn- strip-zone-name
  "Strips the time zone name from a timestamp."
  [timestamp]
  (string/replace timestamp #"\s*\([^\)]*\)$" ""))

(defn format-timestamp
  "Formats a timestamp that may be represented as an already formatted
   timestamp or as a string representing the number of milliseconds since the
   epoch"
  [timestamp]
  (let [timestamp (str timestamp)]
    (if (re-matches #"\d+" timestamp)
      (unparse date-formatter (DateTime. (Long/parseLong timestamp)))
      (unparse date-formatter (parse date-parser (strip-zone-name timestamp))))))

(defn parse-timestamp
  "Parses a timestamp that is in a format similar to the default date and time
   format used by JavaScript.  According to the Joda Time API documentation,
   time zone names are not parseable.  These timestamps already contain the
   time zone offset, however, so the time zone names are redundant.  The
   solution is to strip the time zone name before attempting to parse the
   timestamp."
  [timestamp]
  (parse date-parser (strip-zone-name timestamp)))

(defn timestamp->millis
  "Converts a timestamp to the number of milliseconds since the epoch."
  [timestamp]
  (cond (number? timestamp)           timestamp
        (re-matches #"\d+" timestamp) (Long/parseLong timestamp)
        :else                         (.getMillis (parse-timestamp timestamp))))

(defn pg-timestamp->millis
  "Converts a PostgreSQL timestamp to the number of milliseconds since the epoch.
   Returns a string."
  [pg-timestamp]
  (str (.getTime pg-timestamp)))

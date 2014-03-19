(ns metadactyl.util.conversions
  (:use [clojure.string :only [blank?]])
  (:import [java.sql Timestamp]))

(defn to-long
  "Converts a string to a long integer."
  [s]
  (try
    (Long/parseLong s)
    (catch Exception e
      (throw (IllegalArgumentException. e)))))

(defn date->long
  "Converts a Date object to a Long representation of its timestamp."
  ([date]
     (date->long date nil))
  ([date default]
     (if (nil? date) default (.getTime date))))

(defn long->timestamp
  "Converts a long value, which may contain an empty string, into an instance
   of java.sql.Timestamp."
  [ms]
  (let [ms (str ms)]
    (when-not (blank? ms) (Timestamp. (to-long ms)))))

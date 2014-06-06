(ns mescal.util
  (:use [clojure.java.io :only [reader]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-time.format :as tf]
            [clojure-commons.error-codes :as ce]))

(defn- assert-defined*
  "Ensures that a symbol is non-nil."
  [symbol-name symbol-value]
  (when (nil? symbol-value)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     (str symbol-name " is nil")})))

(defmacro assert-defined
  "Ensures that zero or more symbols are defined."
  [& syms]
  `(do ~@(map (fn [sym] `(@#'assert-defined* ~(name sym) ~sym)) syms)))

(defn decode-json
  "Parses a JSON stream or string."
  [source]
  (if (string? source)
    (cheshire/decode source true)
    (cheshire/decode-stream (reader source) true)))

(defn parse-timestamp
  "Converts a formatted timestamp to the number of milliseconds since the epoch."
  [timestamp]
  (when-not (nil? timestamp)
    (.getMillis (tf/parse (:date-time tf/formatters) timestamp))))

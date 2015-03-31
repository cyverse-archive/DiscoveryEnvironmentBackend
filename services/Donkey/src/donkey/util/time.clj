(ns donkey.util.time
  (:require [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [clojure.string :as string]))

(defn format-timestamp
  "Formats a timestamp in a standard format."
  [timestamp]
  (if-not (or (string/blank? timestamp) (= "0" timestamp))
    (tf/unparse (:date-time tf/formatters) (tc/from-long (Long/parseLong timestamp)))
    ""))

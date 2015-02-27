(ns jex.json-validator
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]
            [clojure.set :as set])
  (:use [clojure.test :only (function?)]))

(defn json?
  "Returns true if a string is JSON."
  [json-string]
  (if (try
        (cheshire/decode json-string)
        (catch Exception e false))
    true
    false))

(defn valid?
  [json-map validators]
  (every? true? (for [vd validators] (vd json-map))))

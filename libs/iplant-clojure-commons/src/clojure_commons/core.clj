(ns clojure-commons.core
  "General-purpose Clojure functions."
  (:use [medley.core :only [remove-vals]]))

(defn remove-nil-values
  "Removes entries containing nil values from a map."
  [m]
  (assert (map? m) "the argument to remove-nil-values must be a map")
  (remove-vals nil? m))

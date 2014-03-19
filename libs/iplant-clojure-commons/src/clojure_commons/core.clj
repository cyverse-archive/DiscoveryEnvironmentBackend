(ns clojure-commons.core
  "General-purpose Clojure functions.")

(defn remove-nil-values
  "Removes entries containing nil values from a map."
  [m]
  (assert (map? m) "the argument to remove-nil-values must be a map")
  (into {} (remove (comp nil? val) m)))

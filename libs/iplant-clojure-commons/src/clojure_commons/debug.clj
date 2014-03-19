(ns clojure-commons.debug
  (:require [clojure.tools.logging :as log]))

(defmacro log-result
  "Logs the result of evaluating a single form."
  [form]
  `(let [res# ~form]
     (log/warn "DEBUG:" (quote ~form) "=" res#)
     res#))

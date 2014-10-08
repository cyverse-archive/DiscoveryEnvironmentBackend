(ns data-info.util.logging
  "This namespace holds the endpoint access logging logic."
  (:require [clojure.tools.logging :as log]))


(defn trace-log
  [trace-type func-name namespace params]
  (let [log-ns (str "trace." namespace)
        desc   (str "[" trace-type "][" func-name "]")
        msg    (apply print-str desc params)]
    (log/log log-ns :trace nil msg)))


(defmacro log-call
  [func-name & params]
  `(trace-log "call" ~func-name ~*ns* [~@params]))


(defn log-func*
  [func-name namespace]
  (fn [result]
    (trace-log "result" func-name namespace result)))


(defmacro log-func
  [func-name]
  `(log-func* ~func-name ~*ns*))


(defmacro log-result
  [func-name & result]
  `(trace-log "result" ~func-name ~*ns* [~@result]))

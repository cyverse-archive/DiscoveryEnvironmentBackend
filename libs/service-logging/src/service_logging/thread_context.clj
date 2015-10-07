(ns service-logging.thread-context
  (:import [org.slf4j MDC]))

(defn set-context!
  "set-context! puts the data from the context-map into the current thread's
  context."
  [context-map]
  (if (map? context-map)
    (doall (map (fn [[k# v#]] (MDC/put (name k#) (str v#))) context-map))))

(defn clear-context!
  "clear-context! clears out context map from the context. This should be
  used when the thread is about to be either shut down or returned to a thread
  pool."
  []
  (MDC/clear))

;; With thanks to https://github.com/vaughnd/clojure-example-logback-integration/
(defmacro with-logging-context
  "Use this to add a map to any logging wrapped in the macro. Macro can be nested.
  (with-logging-context {:key \"value\"} (log/info \"yay\"))
  "
  [context & body]
  `(let [wrapped-context# ~context
         ctx# (MDC/getCopyOfContextMap)]
     (try
       (set-context! wrapped-context#)
       ~@body
       (finally
         (if ctx#
           (MDC/setContextMap ctx#)
           (clear-context!))))))

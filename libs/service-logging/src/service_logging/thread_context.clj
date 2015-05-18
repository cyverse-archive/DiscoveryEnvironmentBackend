(ns service-logging.thread-context
  (:import [org.apache.logging.log4j ThreadContext]))

(defn set-context!
  "set-context! puts the data from the context-map into the current thread's
  ThreadContext."
  [context-map]
  (doseq [[k v] (seq context-map)]
    (ThreadContext/put (name k) v)))

(defn clear-context!
  "clear-context! clears out context map from the ThreadContext. This should be
  used when the thread is about to be either shut down or returned to a thread
  pool."
  []
  (ThreadContext/clearMap))

(defn wrap-thread-context
  "wrap-thread-context is a ring handler that puts the data in the context-map
  into the request thread's ThreadContext."
  [handler context-map]
  (fn [request]
    (set-context! context-map)
    (let [resp (handler request)]
      (clear-context!)
      resp)))

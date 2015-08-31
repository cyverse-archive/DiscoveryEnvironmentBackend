(ns service-logging.thread-context
  (:import [org.slf4j MDC]))


(defn set-context!
  "set-context! puts the data from the context-map into the current thread's
  ThreadContext."
  [context-map]
  (doseq [[k v] (seq context-map)]
    (MDC/put (name k) v)))

(defn clear-context!
  "clear-context! clears out context map from the ThreadContext. This should be
  used when the thread is about to be either shut down or returned to a thread
  pool."
  []
  (MDC/clear))

(defn wrap-thread-context
  "wrap-thread-context is a ring handler that puts the data in the context-map
  into the request thread's ThreadContext."
  [handler context-map]
  (fn [request]
    (set-context! context-map)
    (let [resp (handler request)]
      (clear-context!)
      resp)))

(defn add-user-to-context
  "add-user-to-context is a ring handler that adds the user value from the query
  string into the ThreadContext with a key of 'username'. The query params
  need to be parsed first."
  [handler]
  (fn [request]
    (let [q-params (:query-params request)]
      (if (contains? q-params "user")
        (set-context! {:user (get (:query-params request) "user")}))
      (handler request))))

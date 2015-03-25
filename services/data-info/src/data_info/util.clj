(ns data-info.util
  "Utility functions for defining services in data-info. This namespace is used by data-info.core
   and several other top-level service definition namespaces."
  (:require [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [try+]]
            [clojure-commons.error-codes :as ce]
            [data-info.util.service :as svc]
            [data-info.util.transformers :as transform]))


(defn- determine-response
  [resp-val]
  (if (and (map? resp-val) (number? (:status resp-val)))
    (svc/data-info-response resp-val (:status resp-val))
    (svc/success-response resp-val)))


(defn trap
  "Traps any exception thrown by a service and returns an appropriate repsonse."
  [f]
  (try+
    (determine-response (f))
    (catch [:error_code ce/ERR_NOT_A_USER] err
      (svc/failure-response err))
    (catch ce/error? err
      (when (>= 500 (ce/get-http-status (:error_code err)))
        (log/error (ce/format-exception (:throwable &throw-context))))
      (svc/error-response err (ce/get-http-status (:error_code err))))
    (catch Throwable o
      (svc/error-response o))
    (catch Object o
      (svc/error-response (Exception. (str "unexpected error: " o))))))


(defn trap-handler
  [handler]
  (fn [req]
    (trap #(handler req))))


(defn req-logger
  [handler]
  (fn [req]
    (log/info "REQUEST:" req)
    (let [resp (handler req)]
      (log/info "RESPONSE:" resp))))


(defn- pre-process-request
  [req & {:keys [slurp?] :or {slurp? false}}]
  (if slurp?
    (assoc req :body (transform/parse-body (slurp (:body req))))
    req))


(defn- ctlr
  [req slurp? func & args]
  (let [req     (pre-process-request req :slurp? slurp?)
        get-arg (fn [arg] (if (keyword? arg) (get req arg) arg))
        argv    (mapv get-arg args)]
    (trap #(apply func argv))))


(defn controller
  [req func & args]
  (let [p (if (contains? (set args) :body)
            (partial ctlr req true func)
            (partial ctlr req false func))]
      (apply p args)))

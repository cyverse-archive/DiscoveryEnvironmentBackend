(ns data-info.util
  "Utility functions for defining services in data-info. This namespace is used by data-info.core
   and several other top-level service definition namespaces."
  (:use [compojure.core]
        [data-info.util.validators :only [parse-body]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]
            [data-info.util.service :as svc]))


(def ^:private uuid-regexes
  [#"^\p{XDigit}{8}(?:-\p{XDigit}{4}){3}-\p{XDigit}{12}$"
   #"^[at]\p{XDigit}{32}"])


(defn is-uuid?
  [id]
  (some #(re-find % id) uuid-regexes))


(defn determine-response
  [resp-val]
  (if (and (map? resp-val) (number? (:status resp-val)))
    (svc/data-info-response resp-val (:status resp-val))
    (svc/success-response resp-val)))

(defn clj-http-error?
  [{:keys [status body]}]
  (and (number? status) ((comp not nil?) body)))


(defn trap
  "Traps any exception thrown by a service and returns an appropriate repsonse."
  [f]
  (try+
    (determine-response (f))
    (catch [:type :error-status] {:keys [res]} res)
    (catch [:type :missing-argument] {:keys [arg]} (svc/missing-arg-response arg))
    (catch [:type :invalid-argument] {:keys [arg val reason]}
      (svc/invalid-arg-response arg val reason))
    (catch [:type :invalid-configuration] {:keys [reason]} (svc/invalid-cfg-response reason))
    (catch [:type :temp-dir-failure] err (svc/temp-dir-failure-response err))
    (catch ce/error? err
      (log/error (ce/format-exception (:throwable &throw-context)))
      (svc/error-response err (ce/get-http-status (:error_code err))))
    (catch IllegalArgumentException e (svc/failure-response e))
    (catch IllegalStateException e (svc/failure-response e))
    (catch Throwable t (svc/error-response t))
    (catch clj-http-error? o o)
    (catch Object o (svc/error-response (Exception. (str "unexpected error: " o))))))


(defn trap-handler
  [handler]
  (fn [req]
    (trap #(handler req))))

(defn req-logger
  [handler]
  (fn [req]
    (log/info "Request received:" req)
    (handler req)))

(defn as-vector
  "Returns the given parameter inside a vector if it's not a vector already."
  [p]
  (cond (nil? p)    []
        (vector? p) p
        :else       [p]))

(defn optional-routes
  "Creates a set of optionally defined routes."
  [[option-fn] & handlers]
  (when (option-fn)
    (apply routes handlers)))


(defn- pre-process-request
  [req & {:keys [slurp?] :or {slurp? false}}]
  (if slurp?
    (assoc req :body (parse-body (slurp (:body req))))
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

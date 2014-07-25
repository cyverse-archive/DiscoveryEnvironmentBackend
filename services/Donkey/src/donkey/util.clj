(ns donkey.util
  "Utility functions for defining services in Donkey. This namespace is used by donkey.core and
   several other top-level service definition namespaces."
  (:use [compojure.core]
        [donkey.util.service]
        [donkey.util.transformers]
        [donkey.util.validators :only [parse-body]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure-commons.error-codes :as ce]
            [clojure.tools.logging :as log]))

(def ^:private uuid-regexes
  [#"^\p{XDigit}{8}(?:-\p{XDigit}{4}){3}-\p{XDigit}{12}$"
   #"^[at]\p{XDigit}{32}"])

(defn is-uuid?
  [id]
  (some #(re-find % id) uuid-regexes))

(defn determine-response
  [resp-val]
  (if (and (map? resp-val) (number? (:status resp-val)))
    (donkey-response resp-val (:status resp-val))
    (success-response resp-val)))

(defn clj-http-error?
  [{:keys [status body]}]
  (and (number? status) ((comp not nil?) body)))

(defn trap
  "Traps any exception thrown by a service and returns an appropriate
   repsonse."
  [f]
  (try+
   (determine-response (f))
   (catch [:type :error-status] {:keys [res]} res)
   (catch [:type :missing-argument] {:keys [arg]} (missing-arg-response arg))
   (catch [:type :invalid-argument] {:keys [arg val reason]}
     (invalid-arg-response arg val reason))
   (catch [:type :invalid-configuration] {:keys [reason]} (invalid-cfg-response reason))
   (catch [:type :temp-dir-failure] err (temp-dir-failure-response err))
   (catch [:type :tree-file-parse-err] err (tree-file-parse-err-response err))
   (catch ce/error? err
     (log/error (ce/format-exception (:throwable &throw-context)))
     (error-response err (ce/get-http-status (:error_code err))))

   (catch IllegalArgumentException e (failure-response e))
   (catch IllegalStateException e (failure-response e))
   (catch Throwable t (error-response t))
   (catch clj-http-error? o o)
   (catch Object o (error-response (Exception. (str "unexpected error: " o))))))

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

(defn flagged-routes
  "Creates a set of routes, removing any nil route definitions."
  [& handlers]
  (apply routes (remove nil? handlers)))

(defn- pre-process-request
  [req & {:keys [slurp?] :or {slurp? false}}]
  (if-not (contains? (:params req) :proxytoken)
      (throw+ {:error_code "ERR_MISSING_PARAM"
               :param "proxyToken"}))
  (let [req (assoc req :params (add-current-user-to-map (:params req)))]
    (if slurp?
      (assoc req :body (parse-body (slurp (:body req))))
      req)))

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

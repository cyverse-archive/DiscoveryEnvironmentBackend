(ns donkey.util
  "Utility functions for defining services in Donkey. This namespace is used by donkey.core and
   several other top-level service definition namespaces."
  (:use [compojure.core]
        [donkey.util.service]
        [donkey.util.transformers]
        [donkey.util.validators :only [parse-body]]
        [slingshot.slingshot :only [try+ throw+]]))

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
  (let [req (assoc req :params (add-current-user-to-map (:params req)))]
    (if slurp?
      (assoc req :body (parse-body (slurp (:body req))))
      req)))

(defn- ctlr
  [req slurp? func & args]
  (let [req     (pre-process-request req :slurp? slurp?)
        get-arg (fn [arg] (if (keyword? arg) (get req arg) arg))
        argv    (mapv get-arg args)]
    (success-response (apply func argv))))

(defn controller
  [req func & args]
  (let [p (if (contains? (set args) :body)
            (partial ctlr req true func)
            (partial ctlr req false func))]
    (apply p args)))

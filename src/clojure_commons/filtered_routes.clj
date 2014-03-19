(ns clojure-commons.filtered-routes
  (:use [clojure.core.incubator :only (-?>)]
        [clojure.string :only (upper-case)]
        [clout.core]
        [compojure.core]
        [compojure.response]))

(defn- method-matches?
  "True if this request matches the supplied request method.  Copied from
   compojure.core because it needs to be accessed in this file."
  [method request]
  (let [request-method (request :request-method)
        form-method (get-in request [:form-params "_method"])]
    (if (and form-method (= request-method :post))
      (= (upper-case (name method)) form-method)
      (= method request-method))))

(defn- if-method
  "Evaluate the handler if the request method matches.  Copied from
   compojure.core because it needs to be accessed in this file."
  [method handler]
  (fn [request]
    (cond
      (or (nil? method) (method-matches? method request))
        (handler request)
      (and (= :get method) (= :head (:request-method request)))
        (-?> (handler request)
          (assoc :body nil)))))

(defn- assoc-route-params
  "Associate route parameters with the request map.  Copied from
   compojure.core because it needs to be accessed in this file."
  [request params]
  (merge-with merge request {:route-params params, :params params}))

(defn- if-route
  "Evaluate the handler if the route matches the request.  Copied from
   compojure.core because it needs to be accessed in this file."
  [route handler]
  (fn [request]
    (if-let [params (route-matches route request)]
      (handler (assoc-route-params request params)))))

(defn- prepare-route
  "Pre-compile the route.  Copied from compojure.core because it needs to be
   accessed in this file."
  [route]
  (cond
    (string? route) `(route-compile ~route)
    (vector? route) `(route-compile
                       ~(first route)
                       ~(apply hash-map (rest route)))
    :else           `(if (string? ~route)
                       (route-compile ~route)
                       ~route)))

(defn- compile-filtered-route
  "Compile a filtered route in the form (method path filter bindings & body)
   into a function.  Copied and adapted from compojure.core/compile-route."
  [method route bindings [filt & filt-args] body]
  `(#'if-method ~method
     (#'if-route ~(prepare-route route)
       (~filt
         (fn [request#]
           (let-request [~bindings request#]
                        (render (do ~@body) request#)))
         ~@filt-args))))

(defmacro FILTERED-GET
  "Generate a filtered GET route."
  [url-path args filt-and-args & body]
  (compile-filtered-route :get url-path args filt-and-args body))

(defmacro FILTERED-POST
  "Generate a filtered POST route."
  [url-path args filt-and-args & body]
  (compile-filtered-route :post url-path args filt-and-args body))

(defmacro FILTERED-PUT
  "Generate a filtered PUT route."
  [url-path args filt-and-args & body]
  (compile-filtered-route :put url-path args filt-and-args body))

(defmacro FILTERED-DELETE
  "Generate a filtered DELETE route."
  [url-path args filt-and-args & body]
  (compile-filtered-route :delete url-path args filt-and-args body))

(defmacro FILTERED-HEAD
  "Generate a filtered HEAD route."
  [url-path args filt-and-args & body]
  (compile-filtered-route :head url-path args filt-and-args body))

(defmacro FILTERED-ANY
  "Generate a filtered route that matches any method."
  [url-path args filt-and-args & body]
  (compile-filtered-route nil url-path args filt-and-args body))

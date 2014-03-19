(ns clojure-commons.lcase-params
  (:require [clojure.string :as string]))

(defn- lcase-params
  [target]
  (cond
   (map? target)    (into {} (for [[k v] target]
                               [(if (string? k) (string/lower-case k) k)
                                (lcase-params v)]))
   (vector? target) (mapv lcase-params target)
   :else            target))

(defn wrap-lcase-params
  "Middleware that converts all parameters to lower case so that they can be treated as effectively
   case-insensitive.  Does not alter the maps under :*-params; these are left with strings."
  [handler]
  (fn [req]
    (handler (update-in req [:params] lcase-params))))

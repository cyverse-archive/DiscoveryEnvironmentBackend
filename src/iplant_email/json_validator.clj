(ns iplant-email.json-validator
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]))

(defn json?
  "Returns true if a string is JSON."
  [json-string]
  (if (try (cheshire/decode json-string true) (catch Exception e false))
    true
    false))

(defn valid?
  "Returns true if the JSON map passed in matches the spec map."
  [json-map spec-map]
  (loop [my-json-map json-map
         my-spec-map spec-map
         is-valid true]
    (let [spec-obj (first my-spec-map)
          spec-key (first spec-obj)
          spec-val (last spec-obj)]
      (cond
        (not (contains? my-json-map spec-key)) false
        :else (let [json-val (get my-json-map spec-key)]
                (cond
                  (fn? spec-val)  (and is-valid (spec-val json-val))
                  (map? spec-val) (recur json-val spec-val is-valid)
                  :else           (and is-valid (== json-val spec-val))))))))

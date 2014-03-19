(ns clavin.environments
  (:use [clojure.java.io :only [reader]]
        [clojure.set :only [union intersection difference]])
  (:require [clojure.string :as string])
  (:import [java.io PushbackReader]))

(def ^:private placeholder-re #"\$\{([^\}]+)\}")

(defn load-envs
  "Loads the environment settings from a specified file."
  [filename]
  (with-open [r (PushbackReader. (reader filename))]
    (binding [*read-eval* false]
      (read r))))

(defn replace-placeholders
  "Replaces placeholders in an environment with the actual values."
  [env]
  (letfn [(gval [k]
            (let [v (env k)]
              (if (nil? v)
                (throw (Exception. (str "bad placeholder: " v)))
                v)))
          (rep [v]
            (string/replace v placeholder-re
                            #(rep (gval (keyword (second %))))))]
    (into {} (map (fn [[k v]] [k (rep v)]) env))))


(defn self-referenced-params
  "Obtains a list of parameters in an environment that are referenced from
   within that environment."
  [env]
  (let [get-param (comp keyword second)]
    (set (mapcat (fn [[_ v]] (map get-param (re-seq placeholder-re (str v)))) env))))

(defn- keyset
  "Obtains a set containing the keys of a map."
  [m]
  (set (keys m)))

(defn- envs-list
  "Grabs the environment mapping objects from the configs."
  [configs]
  (map keyset (vals configs)))

(defn- extract-envs
  "Extracts a set of all environment mapping objects found in all properties."
  [configs]
  (apply union (envs-list configs)))

(defn- envs-valid?
  "Verifies that all of the properties have the same set of environments
   defined."
  [configs]
  (apply = (envs-list configs)))

(defn- add-incomplete-prop
  "Adds prop to the list of incomplete-props if its environments don't match
   those given in all-envs."
  [incomplete-props prop configs all-envs]
  (if (seq (difference all-envs (keyset (prop configs))))
    (conj incomplete-props prop)
    incomplete-props))

(defn- invalid-keys
  "Determines which properties are invalid (that is, not defined with all
   environments) in the given configs."
  [configs]
  (let [props (keys configs)
        all-envs (extract-envs configs)]
    (reduce #(add-incomplete-prop %1 %2 configs all-envs) [] props)))

(defn- missing-envs
  "Determines which environments are missing from any of the defined properties."
  [configs]
  (let [env-keys (envs-list configs)]
    (difference (extract-envs configs) (apply intersection env-keys))))

(defn show-configs-invalid-msg
  "Prints a message indicating that a configs file is not valid."
  [configs filename]
  (println filename "is not valid.")
  (println)
  (println "Please check the following properties for the following environments:")
  (dorun (map #(println (str " " %)) (sort (invalid-keys configs))))
  (dorun (map #(println (str "\t" %)) (sort (missing-envs configs)))))

(defn validate-envs
  "Ensures that all properties have the same set of environments defined."
  [filename]
  (let [configs (load-envs filename)]
    (if (envs-valid? configs)
      (println filename "is valid.")
      (show-configs-invalid-msg configs filename))))

(defn env-configs
  "Gets a map of properties where keys are from envs and the values are
   extracted from the sub-map values in envs using the given env-name and
   deployment."
  [configs env-name deployment]
  (let [env-key (map keyword [env-name deployment])]
    (into {} (for [[k v] configs] [k (v env-key)]))))

(defn- env-names
  "Obtains the list of environment names from the configs map."
  [envs]
  (map #(map name %) (keys (first (vals envs)))))

(defn list-envs
  "Lists all of the environments defined in an environment file."
  [filename]
  (let [hdrs  ["environment" "deployment"]
        envs  (load-envs filename)
        names (env-names envs)
        width (apply max (map count (apply concat (conj names hdrs))))
        sep   (apply str (take width (repeat "-")))
        fcol  (fn [v w] (apply str v (take (- w (count v)) (repeat " "))))
        fcols (fn [vs w] (map #(fcol % w) vs))]
    (apply println (fcols hdrs width))
    (apply println (fcols [sep sep] width))
    (dorun (map (partial apply println) (map #(fcols % width) names)))))

(defn env-for-dep
  "Determines the name of the environment associated with a deployment name."
  [configs dep]
  (let [names (map first (filter #(= dep (second %)) (env-names configs)))]
    (when (empty? names)
      (throw (Exception. (str "no environment found for deployment " dep))))
    (when (> (count names) 1)
      (throw
       (Exception. (str "multiple environments found for deployment " dep))))
    (first names)))

(defn envs-by-dep
  "Obtains a list of environments organized by environment and deployment."
  [envs]
  (map (fn [[env dep]]
         (conj (vec [env dep]) (env-configs envs env dep)))
       (env-names envs)))

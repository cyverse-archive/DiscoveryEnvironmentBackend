(ns clavin.edn-filters
  (:use [medley.core]
        [clojure.set]
        [clojure.java.io :only [file]])
  (:require [clojure.edn :as edn]
            [me.raynes.fs :as fs]
            [clojure.string :as string]))

(defn load-filter
  "Loads an EDN filter from disk. EDN filters must be valid EDN files."
  [filter-path]
  (edn/read-string (slurp (file filter-path))))

(defn list-filters
  "Returns a list of all .edn files in the filter directory."
  [filter-dir]
  (map #(.getAbsolutePath %)
       (fs/find-files* filter-dir #(.endsWith (.getName %) ".edn"))))

(defn read-environments
  [path-to-file]
  (edn/read-string (slurp (file path-to-file))))

(defn map-for-deployment
  "Constructs a full map for a particular deployment.
   dep-tuple should look like [:prod :prod]."
  [dep-tuple e]
  (into {}
    (map (fn [t] [(first t) (first (vals (filter-keys #(= % dep-tuple) (second t))))])
         (seq e))))

(defn- rename-keys-in-map
  [in-map xform-map]
  (if (contains? xform-map :rename)
    (rename-keys in-map (:rename xform-map))
    in-map))

(defn apply-xforms
  [in-map xform-map]
  (-> in-map
      (rename-keys-in-map xform-map)))

(defn generate-map
  "Takes in a tuple of the format '[environment deployment] and a
   filter structure. Returns the new map."
  [dep-tuple environments filter-file]
  (let [dep-map  (map-for-deployment dep-tuple environments)
        map-keys (first filter-file)
        xforms   (if (second filter-file) (second filter-file) {})]
    (apply-xforms (filter-keys #(contains? (set map-keys) %) dep-map) xforms)))

(defn pprint-to-string
  [m]
  (let [sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (clojure.pprint/pprint m))
    (str sw)))

(defn spit-edn-files
  [dep-tuple envs-path filter-dir dest-dir]
  (let [envs    (read-environments envs-path)
        filters (list-filters filter-dir)]
    (doseq [f filters]
      (spit
       (file dest-dir (fs/base-name f))
       (pprint-to-string (generate-map dep-tuple envs (load-filter f))))
      (println "Wrote out" (str (file dest-dir (fs/base-name f)))))))

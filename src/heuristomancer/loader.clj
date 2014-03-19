(ns heuristomancer.loader
  (:use [clojure.java.io :only [reader]])
  (:require [instaparse.core :as insta])
  (:import [java.io PushbackReader]))

(def ^:private grammar-list-file "grammars.clj")

(defn resource-reader
  "Obtains a reader for a named resource."
  [resource-name]
  (-> (Thread/currentThread)
      (.getContextClassLoader)
      (.getResourceAsStream resource-name)
      (reader)))

(defn resource-stream
  "Obtains an input stream for a named resource."
  [resource-name]
  (-> (Thread/currentThread)
    (.getContextClassLoader)
    (.getResourceAsStream resource-name)))

(defn- load-grammar-list
  "Loads the list of format names and grammars from the grammar list file."
  []
  (with-open [r (PushbackReader. (resource-reader grammar-list-file))]
    (binding [*read-eval* false]
      (read r))))

(defn- slurpee
  "Slurps in a parser that's stored as a resource."
  [resource-path]
  (insta/parser (slurp (resource-reader resource-path))))

(defn get-parser
  "Loads the parser with the given format name."
  [format-name]
  (let [resource-path (second (first (filter (fn [[k _]] (= k format-name)) (load-grammar-list))))]
    (when-not (nil? resource-path)
      (slurpee resource-path))))

(defn- load-parser
  "Loads a single parser for a format name and resource path."
  [[format-name resource-path sip-selector]]
  (vector format-name (slurpee resource-path) sip-selector))

(defn load-parsers
  "Loads the list of parsers to try when identifying file types."
  []
  (doall (map load-parser (load-grammar-list))))

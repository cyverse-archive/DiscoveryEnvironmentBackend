(ns facepalm.c180-2013031301
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130313.01")

(defn- add-additional-tool-architectures
  "Adds some new tool architectures to the list."
  []
  (println "\t* adding some new tool architectures")
  (insert :tool_architectures
          (values
           [{:name        "Others"
             :description "Another specific architecture."}
            {:name        "Don't know"
             :description "Used in cases where the user doesn't know the architecture."}])))

(defn convert
  "Performs the database conversion for DE version 1.8.0:20130313.01."
  []
  (println "Performing conversion for" version)
  (add-additional-tool-architectures))

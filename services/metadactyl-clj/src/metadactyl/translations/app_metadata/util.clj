(ns metadactyl.translations.app-metadata.util
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.set :as set]))

(def input-property-types
  #{"Input" "FileInput" "FolderInput" "MultiFileSelector"})

(def output-property-types
  #{"Output" "FileOutput" "FolderOutput" "MultiFileOutput"})

(def ref-genome-property-types
  #{"ReferenceAnnotation" "ReferenceGenome" "ReferenceSequence"})

(def io-property-types
  (set/union input-property-types output-property-types ref-genome-property-types))

(def ^:private input-multiplicities-and-prop-types
  [["FileInput"         "One"]
   ["FolderInput"       "Folder"]
   ["MultiFileSelector" "Many"]])

(def ^:private output-multiplicities-and-prop-types
  [["FileOutput"      "One"]
   ["FolderOutput"    "Folder"]
   ["MultiFileOutput" "Many"]])

(def ^:private input-multiplicity-for
  (into {} input-multiplicities-and-prop-types))

(def ^:private input-property-type-for
  (into {} (map (comp vec reverse) input-multiplicities-and-prop-types)))

(def ^:private output-multiplicity-for
  (into {} output-multiplicities-and-prop-types))

(def ^:private output-property-type-for
  (into {} (map (comp vec reverse)  output-multiplicities-and-prop-types)))

(defn multiplicity-for
  [prop-type mult]
  (cond
   (input-property-types prop-type)      (input-multiplicity-for prop-type mult)
   (output-property-types prop-type)     (output-multiplicity-for prop-type mult)
   (ref-genome-property-types prop-type) "One"
   :else                                 mult))

(defn property-type-for
  [prop-type mult info-type]
  (cond
   (ref-genome-property-types info-type) info-type
   (input-property-types prop-type)      (input-property-type-for mult prop-type)
   (output-property-types prop-type)     (output-property-type-for mult prop-type)
   :else                                 prop-type))

(defn generic-property-type-for
  [prop-type]
  (cond
   (input-property-types prop-type)      "Input"
   (output-property-types prop-type)     "Output"
   (ref-genome-property-types prop-type) "Input"
   :else                                  prop-type))

(defn get-property-groups
  "Gets the list of property groups "
  [template]
  (cond
   (map? (:groups template))        (get-property-groups (:groups template))
   (sequential? (:groups template)) (:groups template)
   :else                            []))

(defn data-obj-type-for
  [prop-type orig-data-obj-type]
  (if (ref-genome-property-types prop-type)
    prop-type
    orig-data-obj-type))

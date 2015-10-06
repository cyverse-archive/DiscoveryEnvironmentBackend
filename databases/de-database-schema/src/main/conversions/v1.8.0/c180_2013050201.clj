(ns facepalm.c180-2013050201
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130502.01")

(defn- deprecate-property-types
  "Deprecates some old property types."
  []
  (println "\t* deprecating some of the older property types.")
  (update :property_type
          (set-fields {:deprecated true})
          (where {:name "Output"})))

(defn- add-new-property-types
  "Inserts some new property types."
  []
  (println "\t* inserting some new property types.")
  (insert :property_type
          (values
           [{:hid           29
             :id            "0E2E3BE4-18A8-487B-BB27-C96A5A5A141F"
             :name          "FileOutput"
             :description   "A single output file."
             :value_type_id 5}
            {:hid           30
             :id            "108011CA-1908-494E-B76F-83BB2BA696D7"
             :name          "FolderOutput"
             :description   "A collection of output files in a single folder."
             :value_type_id 5}
            {:hid           31
             :id            "8EF87E50-460F-402A-B5C8-BFBB83211A54"
             :name          "MultiFileOutput"
             :description   "Multiple output files matched by a glob pattern."
             :value_type_id 5}])))

(def ^:private property-type-order
  (map (fn [prop-type index] [prop-type index])
       ["Text" "MultiLineText" "Flag" "Integer" "Double" "TextSelection" "IntegerSelection"
        "DoubleSelection" "TreeSelection" "EnvironmentVariable" "FileInput" "FolderInput"
        "MultiFileSelector" "FileOutput" "FolderOutput" "MultiFileOutput"]
       (iterate inc 1)))

(defn- reorder-property-types
  "Reorders all of the existing property types."
  []
  (println "\t* updating the property type display order.")
  (update :property_type
          (set-fields {:display_order 999}))
  (dorun
   (map (fn [[n o]]
          (update :property_type
                  (set-fields {:display_order o})
                  (where {:name n})))
        property-type-order)))

(defn convert
  "Performs the conversion for database version 1.8.0:20130502.01."
  []
  (println "Performing conversion for" version)
  (deprecate-property-types)
  (add-new-property-types)
  (reorder-property-types))

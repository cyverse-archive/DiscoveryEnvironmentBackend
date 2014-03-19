(ns facepalm.c180-2013041901
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130419.01")

(defn- deprecate-property-types
  "Deprecates some old property types."
  []
  (println "\t* deprecating some of the older property types.")
  (update :property_type
          (set-fields {:deprecated true})
          (where {:name [in ["Number" "Selection" "ValueSelection" "Input"]]})))

(defn- add-new-property-types
  "Inserts some new property types."
  []
  (println "\t* inserting some new property types.")
  (insert :property_type
          (values
           [{:hid           21
             :id            "C389D80A-F94E-4904-B6EF-BD658A18FC8A"
             :name          "Integer"
             :description   "An integer value."
             :value_type_id 3}
            {:hid           22
             :id            "01250DB2-F8E9-4D9E-B82E-C4713DA84068"
             :name          "Double"
             :description   "A real number value."
             :value_type_id 3}
            {:hid           23
             :id            "C529C00A-8B6F-4B73-80DA-C460C09722ED"
             :name          "TextSelection"
             :description   "A list for selecting a textual value."
             :value_type_id 1}
            {:hid           24
             :id            "0F4E0460-893B-4724-BC7C-D145575B9B73"
             :name          "IntegerSelection"
             :description   "A list for selecting an integer value."
             :value_type_id 3}
            {:hid           25
             :id            "B8566277-C368-40E9-8B66-BC1C884CF69B"
             :name          "DoubleSelection"
             :description   "A list for selecting a real number value."
             :value_type_id 3}
            {:hid           26
             :id            "3B3FAD4C-691B-44A8-BF34-D406F9052239"
             :name          "FileInput"
             :description   "A control allowing for the selection of a single file."
             :value_type_id 4}
            {:hid           27
             :id            "9633FD4C-5FFC-4471-B531-2ECAAA683E26"
             :name          "FolderInput"
             :description   "A control allowing for the selection of an entire folder."
             :value_type_id 4}
            {:hid           28
             :id            "FD5C9D3E-663D-469C-9455-5EE59621BF0E"
             :name          "MultiFileSelector"
             :description   "A control allowing for the selection of multiple files."
             :value_type_id 4}])))

(def ^:private property-type-order
  [["Text"                1]
   ["MultiLineText"       2]
   ["Flag"                3]
   ["Integer"             4]
   ["Double"              5]
   ["TextSelection"       6]
   ["IntegerSelection"    7]
   ["DoubleSelection"     8]
   ["TreeSelection"       9]
   ["EnvironmentVariable" 10]
   ["Output"              11]
   ["FileInput"           12]
   ["FolderInput"         13]
   ["MultiFileSelector"   14]])

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
  "Performs the conversion for database version 1.8.0:20130419.01."
  []
  (println "Performing conversion for" version)
  (deprecate-property-types)
  (add-new-property-types)
  (reorder-property-types))

(ns facepalm.c160-2012120401
  (:use [korma.core]
        [kameleon.core])
  (:require [clojure.string :as string]))

(def ^:private version
  "The destination database version."
  "1.6.0:20121204.01")

(defn- remove-public-apps-from-workspaces
  "Removes any apps that are marked as public from any user's 'Apps under development' group."
  []
  (println "\t* removing public apps from users' workspaces.")
  (delete :template_group_template
          (where {(subselect :template_group
                             (fields :name)
                             (where {:template_group_template.template_group_id
                                     :template_group.hid}))
                  "Applications under development"})
          (where {(subselect :analysis_listing
                             (fields :is_public)
                             (where {:template_group_template.template_id
                                     :analysis_listing.hid}))
                  true})))

(defn- subselect-property-type-hid
  "A subselect query that obtains the internal ID of a property type with the given name."
  [type-name]
  (subselect :property_type
             (fields :hid)
             (where {:name type-name})))

(defn- quote-str
  "Quotes a string."
  [s]
  (str \" s \"))

(defn change-property-types
  "Changes properties of one type to properties of another type."
  [old-type new-type]
  (println "\t* changing properties of type" (quote-str old-type) "to properties of type"
           (quote-str new-type))
  (update :property
          (set-fields {:property_type (subselect-property-type-hid new-type)})
          (where {:property_type (subselect-property-type-hid old-type)})))

(defn deprecate-property-types
  "Deprecates the property types with the given names."
  [& names]
  (println "\t* deprecating property types:" (string/join ", " (map quote-str names)))
  (update :property_type
          (set-fields {:deprecated true})
          (where {:name [in names]})))

(defn convert
  "Performs the conversions for database version 1.6.0:20121204.01"
  []
  (println "Performing the conversion for" version)
  (remove-public-apps-from-workspaces)
  (change-property-types "XBasePairs" "Number")
  (change-property-types "XBasePairsText" "Text")
  (change-property-types "QuotedText" "Text")
  (deprecate-property-types "XBasePairs" "XBasePairsText" "QuotedText"))

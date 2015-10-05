(ns facepalm.c187-2014042301
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.7:20140423.01")

(def ^:private migs-attributes
  [["A378D704-A979-448A-A1A9-52B94B2592F5"
    "Metadata complete"
    "Check this only when you have entered all required fields."
    false,
    "8130EC25-2452-4FF0-B66A-D9D3A6350816"]])

(def ^:private attribute-ids
  [["59bd3d26-34d5-4e75-99f5-840a20089caf", "A378D704-A979-448A-A1A9-52B94B2592F5", 35]
   ["40ac191f-bb36-4f4e-85fb-8b50abec8e10", "A378D704-A979-448A-A1A9-52B94B2592F5", 42]
   ["f52a4d57-00af-43ec-97d5-1c7e7779f6c3", "A378D704-A979-448A-A1A9-52B94B2592F5", 41]])

(defn- str->uuid
  "Converts a string representation of a UUID to a UUID class."
  [s]
  (UUID/fromString s))

(defn- add-metadata-tmpl-attrs
  "Adds an identifier to a metadata attribute."
  [[template-id attribute-id display-order]]
  (insert :metadata_template_attrs
          (values {:template_id   (str->uuid template-id)
                   :attribute_id  (str->uuid attribute-id)
                   :display_order display-order})))

(defn- add-metadata-attributes
  "Adds metadata attributes to the database."
  [[attribute-id attr-name attr-desc required? type-id]]
  (insert :metadata_attributes
          (values {:id            (str->uuid attribute-id)
                   :name          attr-name
                   :description   attr-desc
                   :required      required?
                   :value_type_id (str->uuid type-id)})))

(defn- add-metadata-attrs
  "Adds the new metadata template attr to the database."
  []
  (println "\t* adding new metadata template attributes")
  (dorun (map add-metadata-attributes migs-attributes)))

(defn- add-metadata-template-attrs
  "Adds the new attrs to the metadata templates"
  []
  (println "\t* adding new attrs to the metadata templates")
  (dorun (map add-metadata-tmpl-attrs attribute-ids)))

(defn convert
  "Performs the conversion for database version 1.8.7:20140423.01"
  []
  (println "Performing the conversion for" version)
  (add-metadata-attrs)
  (add-metadata-template-attrs))

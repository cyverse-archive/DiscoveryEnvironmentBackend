(ns facepalm.c184-2013121701
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.4:20131217.01")

(defn- add-metadata-value-types-table
  "Adds the table that stores the metadata value types that are available to the DE."
  []
  (println "\t* adding the metadata_value_types table")
  (exec-raw
   "CREATE TABLE metadata_value_types (
    id uuid NOT NULL,
    name varchar(64) NOT NULL,
    PRIMARY KEY (id))"))

(defn- uuid
  "Converts a string representation of a UUID sto a UUID class."
  [s]
  (UUID/fromString s))

(def ^:private metadata-value-types
  [["4CB79D83-E694-4ACF-AA60-DDADEE087B24", "Timestamp"],
   ["8130EC25-2452-4FF0-B66A-D9D3A6350816", "Boolean"],
   ["29F9F4FD-594C-493D-9560-FE8851084870", "Number"],
   ["C6CB42CD-7C47-47A1-8704-F6582B510ACF", "Integer"],
   ["C29B0B10-D660-4582-9EB7-40C4F1699DD6", "String"],
   ["127036FF-EF19-4665-A9A9-7A6878D9813A", "Multiline Text"],
   ["28A1F81A-8B4F-4940-BCD4-E39241BF15DC", "URL/URI"]])

(defn- populate-metadata-value-types-table
  "Adds the metadata value types to the database."
  []
  (println "\t* populating the metadata_value_types table")
  (-> (insert* :metadata_value_types)
      (values (map (fn [[id name]] {:id (uuid id) :name name}) metadata-value-types))
      (insert)))

(defn- add-metadata-templates-table
  "Adds the table that stores the metadata templates."
  []
  (println "\t* adding the metadata_templates table")
  (exec-raw
   "CREATE TABLE metadata_templates (
    id uuid NOT NULL,
    name varchar(64) NOT NULL,
    deleted boolean DEFAULT FALSE NOT NULL,
    PRIMARY KEY (id))"))

(defn- add-metadata-attributes-table
  "Adds the table that stores the metadata attributes that appear in a template."
  []
  (println "\t* adding the metadata_attributes table")
  (exec-raw
   "CREATE TABLE metadata_attributes (
    template_id uuid NOT NULL REFERENCES metadata_templates(id),
    name varchar(64) NOT NULL,
    description varchar(1024) NOT NULL,
    required boolean NOT NULL,
    display_order integer NOT NULL,
    value_type_id uuid NOT NULL REFERENCES metadata_value_types(id))")
  (exec-raw
   "CREATE INDEX metadata_attributes_template_id
    ON metadata_attributes(template_id)"))

(def ^:private initial-templates
  [["59BD3D26-34D5-4E75-99F5-840A20089CAF", "iDS Genome Sequences"]])

(def ^:private initial-attributes
  [["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "project",
    "project name",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "specimen identifier",
    "specimen ID (e.g. herbarium or museum specimen ID)",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "collection date",
    "the date of collection in an ISO8601 compliant format",
    true,
    "4CB79D83-E694-4ACF-AA60-DDADEE087B24"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "geographic location name - country/sea",
    "country or sea name",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "geographic location name - region",
    "region name",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "geographic location name - specific location",
    "specific location name",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "geographic location lattitude",
    "lattitude of location in decimal degrees",
    false,
    "29F9F4FD-594C-493D-9560-FE8851084870"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "geographic location longitude",
    "longitude of location in decimal degrees",
    false,
    "29F9F4FD-594C-493D-9560-FE8851084870"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "genus",
    "use NCBI taxon ID or text",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "species",
    "use NCBI taxon ID or text",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "infraspecific name",
    "use NCBI taxon ID or text",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "infraspecific rank",
    "use only if using \"infraspecific name\"",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "subspecific lineage",
    "name of stock or cultivar; suggest use of controlled vocabulary",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "organ/tissue",
    "suggest use of anatomy ontology ID",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "developmental stage",
    "suggest use of development stage ontology ID",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "ploidy",
    "",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "haploid chromosomes count",
    "",
    false,
    "C6CB42CD-7C47-47A1-8704-F6582B510ACF"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "estimated genome size",
    "in #basepairs",
    false,
    "C6CB42CD-7C47-47A1-8704-F6582B510ACF"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "sequencing method",
    "",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "sequencing date",
    "date of sequencing in ISO8601 compliant format",
    true,
    "4CB79D83-E694-4ACF-AA60-DDADEE087B24"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "sequencing hardware",
    "",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "sequencing software version",
    "",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "sequencing chemistry version",
    "",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "Q-score ref",
    "",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "multiplex identifiers",
    (str "report sequence of mole3cular barcode used to specifically tag unique samples in a "
         "sequencing run"),
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "adapters",
    "provide the adapter sequences",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "submitted to archive",
    "Has the sequence been submitted to an INSDC archive?",
    true,
    "8130EC25-2452-4FF0-B66A-D9D3A6350816"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "library construction method",
    "",
    true,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "assembly - method",
    "",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "assembly - estimated error rate",
    "",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "assembly - method of calculation",
    "",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "finishing strategy - status",
    "",
    false,
    "C29B0B10-D660-4582-9EB7-40C4F1699DD6"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "finishing strategy - coverage",
    "",
    false,
    "C6CB42CD-7C47-47A1-8704-F6582B510ACF"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "finishing strategy - coverage",
    "",
    false,
    "C6CB42CD-7C47-47A1-8704-F6582B510ACF"],
   ["59BD3D26-34D5-4E75-99F5-840A20089CAF",
    "finishing strategy - number of contigs",
    "",
    false,
    "C6CB42CD-7C47-47A1-8704-F6582B510ACF"]])

(defn- add-initial-templates
  "Adds the initial metadata templates to the database."
  []
  (println "\t* adding the initial metadata templates")
  (-> (insert* :metadata_templates)
      (values (map (fn [[id name]] {:id (uuid id) :name name}) initial-templates))
      (insert))
  (-> (insert* :metadata_attributes)
      (values (map (fn [[template-id name desc required? value-type-id] display-order]
                     {:template_id   (uuid template-id)
                      :name          name
                      :description   desc
                      :required      required?
                      :value_type_id (uuid value-type-id)
                      :display_order display-order})
                   initial-attributes
                   (range)))
      (insert)))

(defn convert
  "Performs the conversion for database version 1.8.4:20131217.01."
  []
  (println "Performing conversion for" version)
  (add-metadata-value-types-table)
  (populate-metadata-value-types-table)
  (add-metadata-templates-table)
  (add-metadata-attributes-table)
  (add-initial-templates))

(ns facepalm.c184-2014011501
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.4:20140115.01")

(defn- add-metadata-attribute-synonyms-function
  "Adds the SQL function used to retrieve all synonyms of a metadata attribute."
  []
  (println "\t* adding the metadata_attribute_synonyms function")
  (exec-raw
   "CREATE OR REPLACE FUNCTION metadata_attribute_synonyms(uuid)
    RETURNS
    TABLE(
        id uuid,
        name varchar(64),
        description varchar(1024),
        required boolean,
        value_type_id uuid
    ) AS $$
        WITH RECURSIVE synonyms(attribute_id, synonym_id) AS (
                SELECT attribute_id, synonym_id
                FROM metadata_attr_synonyms
            UNION
                SELECT s.attribute_id AS attribute_id,
                       s0.synonym_id AS synonym_id
                FROM metadata_attr_synonyms s, synonyms s0
                WHERE s0.attribute_id = s.synonym_id
        )
        SELECT a.id, a.name, a.description, a.required, a.value_type_id
        FROM (
                SELECT synonym_id AS id FROM synonyms
                WHERE attribute_id = $1
                AND synonym_id != $1
            UNION
                SELECT attribute_id AS id FROM synonyms
                WHERE synonym_id = $1
                AND synonym_id != $1
        ) AS s
        JOIN metadata_attributes a ON s.id = a.id
    $$ LANGUAGE SQL"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (add-metadata-attribute-synonyms-function))

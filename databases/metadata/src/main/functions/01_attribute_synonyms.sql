SET search_path = public, pg_catalog;

--
-- A function that recursively finds all of the synonyms of a metadata attribute.
--
CREATE OR REPLACE FUNCTION attribute_synonyms(uuid)
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
            FROM attr_synonyms
        UNION
            SELECT s.attribute_id AS attribute_id,
                   s0.synonym_id AS synonym_id
            FROM attr_synonyms s, synonyms s0
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
    JOIN attributes a ON s.id = a.id
$$ LANGUAGE SQL;

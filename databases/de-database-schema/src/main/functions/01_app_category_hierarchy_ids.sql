SET search_path = public, pg_catalog;

--
-- A function that obtains the internal app category identifiers for the
-- app category hierarchy rooted at the category with the given identifier.
--
CREATE FUNCTION app_category_hierarchy_ids(character varying(255)) RETURNS TABLE(id character varying(255)) AS $$
    WITH RECURSIVE subcategories(parent_id) AS
    (
            SELECT acg.parent_category_id AS parent_id, ac.id
            FROM app_category_group acg
            RIGHT JOIN app_categories ac ON acg.child_category_id = ac.id
            WHERE ac.id = $1
        UNION ALL
            SELECT acg.parent_category_id AS parent_id, ac.id
            FROM subcategories sc, app_category_group acg
            JOIN app_categories ac ON acg.child_category_id = ac.id
            WHERE acg.parent_category_id = sc.id
    )
    SELECT id FROM subcategories
$$ LANGUAGE SQL;

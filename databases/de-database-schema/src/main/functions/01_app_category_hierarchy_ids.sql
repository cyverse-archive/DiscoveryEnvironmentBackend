SET search_path = public, pg_catalog;

--
-- A function that obtains the internal app category identifiers for the
-- app category hierarchy rooted at the category with the given identifier.
--
CREATE FUNCTION app_category_hierarchy_ids(bigint) RETURNS TABLE(hid bigint) AS $$
    WITH RECURSIVE subcategories(parent_hid) AS
    (
            SELECT tgg.parent_group_id AS parent_hid, ac.hid
            FROM template_group_group tgg
            RIGHT JOIN app_categories ac ON tgg.subgroup_id = ac.hid
            WHERE ac.hid = $1
        UNION ALL
            SELECT tgg.parent_group_id AS parent_hid, ac.hid
            FROM subcategories sc, template_group_group tgg
            JOIN app_categories ac ON tgg.subgroup_id = ac.hid
            WHERE tgg.parent_group_id = sc.hid
    )
    SELECT hid FROM subcategories
$$ LANGUAGE SQL;

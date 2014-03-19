SET search_path = public, pg_catalog;

--
-- A function that obtains the internal analysis category identifiers for the
-- analysis group hierarchy rooted at the category with the given identifier.
--
CREATE FUNCTION app_group_hierarchy_ids(bigint) RETURNS TABLE(hid bigint) AS $$
    WITH RECURSIVE subcategories(parent_hid) AS
    (
            SELECT tgg.parent_group_id AS parent_hid, tg.hid
            FROM template_group_group tgg
            RIGHT JOIN template_group tg ON tgg.subgroup_id = tg.hid
            WHERE tg.hid = $1
        UNION ALL
            SELECT tgg.parent_group_id AS parent_hid, tg.hid
            FROM subcategories sc, template_group_group tgg
            JOIN template_group tg ON tgg.subgroup_id = tg.hid
            WHERE tgg.parent_group_id = sc.hid
    )
    SELECT hid FROM subcategories
$$ LANGUAGE SQL;

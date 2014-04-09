SET search_path = public, pg_catalog;

--
-- A function that returns all of the app categories underneath the one
-- with the given identifier.
--
CREATE FUNCTION app_category_hierarchy(bigint)
RETURNS
TABLE(
    parent_hid bigint,
    hid bigint,
    id varchar(255),
    name varchar(255),
    description varchar(255),
    workspace_id bigint,
    is_public boolean,
    app_count bigint
) AS $$
    WITH RECURSIVE subcategories AS
    (
            SELECT tgg.parent_group_id AS parent_hid, ac.hid, ac.id, ac.name,
                   ac.description, ac.workspace_id, ac.is_public,
                   app_count(ac.hid) AS app_count
            FROM template_group_group tgg
            RIGHT JOIN app_category_listing ac ON tgg.subgroup_id = ac.hid
            WHERE ac.hid = $1
        UNION ALL
            SELECT tgg.parent_group_id AS parent_hid, ac.hid, ac.id, ac.name,
                   ac.description, ac.workspace_id, ac.is_public,
                   app_count(ac.hid) AS app_count
            FROM subcategories sc, template_group_group tgg
            JOIN app_category_listing ac ON tgg.subgroup_id = ac.hid
            WHERE tgg.parent_group_id = sc.hid
    )
    SELECT * FROM subcategories
$$ LANGUAGE SQL;

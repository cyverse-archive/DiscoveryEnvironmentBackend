SET search_path = public, pg_catalog;

--
-- A function that returns all of the app categories underneath the one
-- with the given identifier.
--
CREATE FUNCTION app_category_hierarchy(varchar(255))
RETURNS
TABLE(
    parent_id varchar(255),
    id varchar(255),
    name varchar(255),
    description varchar(255),
    workspace_id bigint,
    is_public boolean,
    app_count bigint
) AS $$
    WITH RECURSIVE subcategories AS
    (
            SELECT acg.parent_category_id AS parent_id, ac.id, ac.name,
                   ac.description, ac.workspace_id, ac.is_public,
                   app_count(ac.id) AS app_count
            FROM app_category_group acg
            RIGHT JOIN app_category_listing ac ON acg.child_category_id = ac.id
            WHERE ac.id = $1
        UNION ALL
            SELECT acg.parent_category_id AS parent_id, ac.id, ac.name,
                   ac.description, ac.workspace_id, ac.is_public,
                   app_count(ac.id) AS app_count
            FROM subcategories sc, app_category_group acg
            JOIN app_category_listing ac ON acg.child_category_id = ac.id
            WHERE acg.parent_category_id = sc.id
    )
    SELECT * FROM subcategories
$$ LANGUAGE SQL;

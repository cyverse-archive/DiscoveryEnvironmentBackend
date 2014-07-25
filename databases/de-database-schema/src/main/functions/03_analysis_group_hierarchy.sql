SET search_path = public, pg_catalog;

--
-- A function that returns all of the analysis groups underneath the one
-- with the given identifier.
--
CREATE FUNCTION analysis_group_hierarchy(bigint)
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
            SELECT tgg.parent_group_id AS parent_hid, ag.hid, ag.id, ag.name,
                   ag.description, ag.workspace_id, ag.is_public,
                   app_count(ag.hid) AS app_count
            FROM template_group_group tgg
            RIGHT JOIN analysis_group_listing ag ON tgg.subgroup_id = ag.hid
            WHERE ag.hid = $1
        UNION ALL
            SELECT tgg.parent_group_id AS parent_hid, ag.hid, ag.id, ag.name,
                   ag.description, ag.workspace_id, ag.is_public,
                   app_count(ag.hid) AS app_count
            FROM subcategories sc, template_group_group tgg
            JOIN analysis_group_listing ag ON tgg.subgroup_id = ag.hid
            WHERE tgg.parent_group_id = sc.hid
    )
    SELECT * FROM subcategories
$$ LANGUAGE SQL;

--
-- Another version of the same function that allows us to indicate whether or
-- not apps containing external steps should be included in the count.
--
CREATE FUNCTION analysis_group_hierarchy(bigint, boolean)
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
            SELECT tgg.parent_group_id AS parent_hid, ag.hid, ag.id, ag.name,
                   ag.description, ag.workspace_id, ag.is_public,
                   app_count(ag.hid, $2) AS app_count
            FROM template_group_group tgg
            RIGHT JOIN analysis_group_listing ag ON tgg.subgroup_id = ag.hid
            WHERE ag.hid = $1
        UNION ALL
            SELECT tgg.parent_group_id AS parent_hid, ag.hid, ag.id, ag.name,
                   ag.description, ag.workspace_id, ag.is_public,
                   app_count(ag.hid, $2) AS app_count
            FROM subcategories sc, template_group_group tgg
            JOIN analysis_group_listing ag ON tgg.subgroup_id = ag.hid
            WHERE tgg.parent_group_id = sc.hid
    )
    SELECT * FROM subcategories
$$ LANGUAGE SQL;

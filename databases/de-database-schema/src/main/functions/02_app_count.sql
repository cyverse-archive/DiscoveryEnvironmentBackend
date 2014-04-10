SET search_path = public, pg_catalog;

--
-- A function that counts the number of apps in an app group hierarchy rooted
-- at the node with the given identifier.
--
CREATE FUNCTION app_count(character varying(255)) RETURNS bigint AS $$
    SELECT COUNT(DISTINCT a.hid) FROM apps a
    JOIN template_group_template tgt on a.hid = tgt.template_id
    WHERE NOT a.deleted
    AND tgt.app_category_id in (SELECT * FROM app_category_hierarchy_ids($1))
$$ LANGUAGE SQL;

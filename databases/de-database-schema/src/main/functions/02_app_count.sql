SET search_path = public, pg_catalog;

--
-- A function that counts the number of apps in an app group hierarchy rooted
-- at the node with the given identifier.
--
CREATE FUNCTION app_count(uuid) RETURNS bigint AS $$
    SELECT COUNT(DISTINCT a.id) FROM apps a
    JOIN app_category_app aca ON a.id = aca.app_id
    WHERE NOT a.deleted
    AND aca.app_category_id IN (SELECT * FROM app_category_hierarchy_ids($1))
$$ LANGUAGE SQL;

--
-- Another version of the same function that allows us to exclude apps
-- containing external steps.
--
CREATE OR REPLACE FUNCTION app_count(uuid, boolean) RETURNS bigint AS $$
    SELECT COUNT(DISTINCT a.id) FROM apps a
    JOIN app_category_app aca ON a.id = aca.app_id
    WHERE NOT a.deleted
    AND aca.app_category_id IN (SELECT * FROM app_category_hierarchy_ids($1))
    AND CASE
        WHEN $2 THEN TRUE
        ELSE NOT EXISTS (
            SELECT * FROM app_steps s
            JOIN tasks t ON t.id = s.task_id
            WHERE s.app_id = a.id
            AND t.external_app_id IS NOT NULL
        )
    END
$$ LANGUAGE SQL;


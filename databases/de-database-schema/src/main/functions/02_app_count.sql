SET search_path = public, pg_catalog;

--
-- A function that counts the number of apps in an app group hierarchy rooted
-- at the node with the given identifier.
--
CREATE FUNCTION app_count(bigint) RETURNS bigint AS $$
    SELECT COUNT(DISTINCT a.hid) FROM transformation_activity a
    JOIN template_group_template tgt ON a.hid = tgt.template_id
    WHERE NOT a.deleted
    AND tgt.template_group_id IN (SELECT * FROM app_group_hierarchy_ids($1))
$$ LANGUAGE SQL;

--
-- Another version of the same function that allows us to exclude apps
-- containing external steps.
--
CREATE OR REPLACE FUNCTION app_count(bigint, boolean) RETURNS bigint AS $$
    SELECT COUNT(DISTINCT a.hid) FROM transformation_activity a
    JOIN template_group_template tgt ON a.hid = tgt.template_id
    WHERE NOT a.deleted
    AND tgt.template_group_id IN (SELECT * FROM app_group_hierarchy_ids($1))
    AND CASE
        WHEN $2 THEN TRUE
        ELSE NOT EXISTS (
            SELECT * FROM transformation_task_steps tts
            JOIN transformation_steps ts ON tts.transformation_step_id = ts.id
            JOIN transformations tx ON ts.transformation_id = tx.id
            WHERE tts.transformation_task_id = a.hid
            AND tx.external_app_id IS NOT NULL
        )
    END
$$ LANGUAGE SQL;

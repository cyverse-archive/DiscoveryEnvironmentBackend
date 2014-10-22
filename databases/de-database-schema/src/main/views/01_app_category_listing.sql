SET search_path = public, pg_catalog;

--
-- app_category_listing view containing the top-level information needed for
-- the app category listing service.
--
CREATE VIEW app_category_listing AS
    SELECT
        c.id,
        c."name",
        c.description,
        c.workspace_id,
        w.is_public
    FROM app_categories c
        LEFT JOIN workspace w ON c.workspace_id = w.id;


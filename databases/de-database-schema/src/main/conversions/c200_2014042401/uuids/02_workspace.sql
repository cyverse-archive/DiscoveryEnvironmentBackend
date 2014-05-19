SET search_path = public, pg_catalog;

--
-- Updates workspace uuid foreign keys.
--
UPDATE app_categories SET workspace_id =
    (SELECT w.id FROM workspace w WHERE workspace_id_v187 = w.id_v187);


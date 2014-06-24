SET search_path = public, pg_catalog;

--
-- Updates columns in the existing tree_urls table.
--
ALTER TABLE ONLY tree_urls ALTER COLUMN id SET DEFAULT uuid_generate_v1();


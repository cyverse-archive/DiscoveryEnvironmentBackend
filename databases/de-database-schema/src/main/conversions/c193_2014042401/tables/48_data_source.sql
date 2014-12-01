SET search_path = public, pg_catalog;

--
-- Updates columns in the existing data_source table.
--
ALTER TABLE ONLY data_source RENAME COLUMN id TO display_order;
ALTER TABLE ONLY data_source RENAME COLUMN uuid TO id;
ALTER TABLE ONLY data_source ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);
ALTER TABLE ONLY data_source ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY data_source ALTER COLUMN description TYPE TEXT;


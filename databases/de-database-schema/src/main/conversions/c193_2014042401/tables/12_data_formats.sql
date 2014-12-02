SET search_path = public, pg_catalog;

--
-- Updates columns in the existing data_formats table.
--
ALTER TABLE ONLY data_formats RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY data_formats RENAME COLUMN guid TO id;
ALTER TABLE ONLY data_formats ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);
ALTER TABLE ONLY data_formats ALTER COLUMN id SET DEFAULT uuid_generate_v1();


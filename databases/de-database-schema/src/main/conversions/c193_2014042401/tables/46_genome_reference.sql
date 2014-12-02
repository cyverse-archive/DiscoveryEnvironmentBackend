SET search_path = public, pg_catalog;

--
-- Updates columns in the existing genome_reference table.
--
ALTER TABLE ONLY genome_reference RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY genome_reference RENAME COLUMN created_by TO created_by_v192;
ALTER TABLE ONLY genome_reference RENAME COLUMN last_modified_by TO last_modified_by_v192;
ALTER TABLE ONLY genome_reference RENAME COLUMN uuid TO id;
ALTER TABLE ONLY genome_reference ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);
ALTER TABLE ONLY genome_reference ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY genome_reference ADD COLUMN created_by UUID;
ALTER TABLE ONLY genome_reference ADD COLUMN last_modified_by UUID;


SET search_path = public, pg_catalog;

--
-- Renames the existing dataobjects table to file_parameters and adds updated columns.
-- rename orderd?
--
ALTER TABLE dataobjects RENAME TO file_parameters;

ALTER TABLE ONLY file_parameters RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN name TO name_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN label TO label_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN description TO description_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN orderd TO orderd_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN switch TO switch_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN required TO required_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN info_type TO info_type_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN data_format TO data_format_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN multiplicity TO multiplicity_v192;
ALTER TABLE ONLY file_parameters RENAME COLUMN data_source_id TO data_source_id_v192;
ALTER TABLE ONLY file_parameters ADD COLUMN id UUID DEFAULT (uuid_generate_v1());
ALTER TABLE ONLY file_parameters ADD COLUMN parameter_id UUID;
ALTER TABLE ONLY file_parameters ADD COLUMN info_type UUID;
ALTER TABLE ONLY file_parameters ADD COLUMN data_format UUID;
ALTER TABLE ONLY file_parameters ADD COLUMN data_source_id UUID;

SET search_path = public, pg_catalog;

--
-- Renames the existing property_type table to parameter_types and adds updated columns.
--
ALTER TABLE property_type RENAME TO parameter_types;

ALTER TABLE ONLY parameter_types RENAME COLUMN hid TO hid_v192;
ALTER TABLE ONLY parameter_types RENAME COLUMN value_type_id TO value_type_id_v192;
ALTER TABLE ONLY parameter_types ALTER COLUMN id TYPE UUID USING
 CAST(regexp_replace(id, '^pt', '') AS UUID);
ALTER TABLE ONLY parameter_types ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY parameter_types ALTER COLUMN description TYPE TEXT;
ALTER TABLE ONLY parameter_types ADD COLUMN value_type_id UUID;

-- The 'Info' parameter type should no longer be marked as deprecated.
UPDATE parameter_types SET deprecated = FALSE WHERE name = 'Info';

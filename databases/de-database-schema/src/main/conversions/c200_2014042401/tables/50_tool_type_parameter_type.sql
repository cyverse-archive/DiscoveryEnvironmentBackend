SET search_path = public, pg_catalog;

--
-- Renames the existing tool_type_property_type table to tool_type_parameter_type and adds updated columns.
-- cols to drop: tool_type_id_v187, property_type_id
--
ALTER TABLE tool_type_property_type RENAME TO tool_type_parameter_type;
ALTER TABLE ONLY tool_type_parameter_type RENAME COLUMN tool_type_id TO tool_type_id_v187;
ALTER TABLE ONLY tool_type_parameter_type ADD COLUMN tool_type_id UUID;
ALTER TABLE ONLY tool_type_parameter_type ADD COLUMN parameter_type_id UUID;


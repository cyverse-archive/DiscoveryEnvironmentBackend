SET search_path = public, pg_catalog;

--
-- Renames obsolete tables.
--
ALTER TABLE transformation_steps RENAME TO transformation_steps_v192;
ALTER TABLE transformations RENAME TO transformations_v192;
ALTER TABLE hibernate_sequence RENAME TO hibernate_sequence_v192;
ALTER TABLE property_group_property RENAME TO property_group_property_v192;
ALTER TABLE template_input RENAME TO template_input_v192;
ALTER TABLE template_output RENAME TO template_output_v192;
ALTER TABLE template_property_group RENAME TO template_property_group_v192;
ALTER TABLE transformation_activity_mappings RENAME TO transformation_activity_mappings_v192;
ALTER TABLE transformation_values RENAME TO transformation_values_v192;
ALTER TABLE validator RENAME TO validator_v192;
ALTER TABLE validator_rule RENAME TO validator_rule_v192;
ALTER TABLE multiplicity RENAME TO multiplicity_v192;

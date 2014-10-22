SET search_path = public, pg_catalog;

--
-- Renames obsolete tables.
--
ALTER TABLE transformation_steps RENAME TO transformation_steps_v187;
ALTER TABLE transformations RENAME TO transformations_v187;
ALTER TABLE hibernate_sequence RENAME TO hibernate_sequence_v187;
ALTER TABLE property_group_property RENAME TO property_group_property_v187;
ALTER TABLE template_input RENAME TO template_input_v187;
ALTER TABLE template_output RENAME TO template_output_v187;
ALTER TABLE template_property_group RENAME TO template_property_group_v187;
ALTER TABLE transformation_activity_mappings RENAME TO transformation_activity_mappings_v187;
ALTER TABLE transformation_values RENAME TO transformation_values_v187;
ALTER TABLE validator RENAME TO validator_v187;
ALTER TABLE validator_rule RENAME TO validator_rule_v187;
ALTER TABLE multiplicity RENAME TO multiplicity_v187;

SET search_path = public, pg_catalog;

--
-- Updates tasks uuid foreign keys.
-- Adds temporary indexes to help speed up the conversion.
--
CREATE INDEX tasks_id_v192_idx ON tasks(id_v192);
CREATE INDEX app_steps_transformation_step_id_idx ON app_steps(transformation_step_id_v192);
CREATE INDEX transformations_template_id_idx ON transformations_v192(template_id);
CREATE INDEX transformation_steps_transformation_id_idx ON transformation_steps_v192(transformation_id);
CREATE INDEX template_property_group_property_group_id_idx ON template_property_group_v192(property_group_id);
UPDATE app_steps SET task_id =
    (SELECT t.id FROM tasks t
     LEFT JOIN transformations_v192 tx ON tx.template_id = t.id_v192
     LEFT JOIN transformation_steps_v192 ts ON ts.transformation_id = tx.id
     WHERE transformation_step_id_v192 = ts.id);
UPDATE parameter_groups g SET task_id =
    (SELECT t.id FROM tasks t
     LEFT JOIN template_property_group_v192 tgt ON tgt.template_id = t.hid_v192
     WHERE property_group_id = g.hid_v192);

-- Drop temporary indexes.
DROP INDEX tasks_id_v192_idx;
DROP INDEX app_steps_transformation_step_id_idx;
DROP INDEX transformations_template_id_idx;
DROP INDEX transformation_steps_transformation_id_idx;
DROP INDEX template_property_group_property_group_id_idx;

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY app_steps ALTER COLUMN task_id SET NOT NULL;
ALTER TABLE ONLY parameter_groups ALTER COLUMN task_id SET NOT NULL;


SET search_path = public, pg_catalog;

--
-- Updates app_steps uuid foreign keys.
--
UPDATE workflow_io_maps SET source_step =
    (SELECT step.id FROM app_steps step
     LEFT JOIN transformation_steps_v192 ts ON ts.id = step.transformation_step_id_v192
     WHERE source_v192 = ts.id);
UPDATE workflow_io_maps SET target_step =
    (SELECT step.id FROM app_steps step
     LEFT JOIN transformation_steps_v192 ts ON ts.id = step.transformation_step_id_v192
     WHERE target_v192 = ts.id);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY workflow_io_maps ALTER COLUMN source_step SET NOT NULL;
ALTER TABLE ONLY workflow_io_maps ALTER COLUMN target_step SET NOT NULL;


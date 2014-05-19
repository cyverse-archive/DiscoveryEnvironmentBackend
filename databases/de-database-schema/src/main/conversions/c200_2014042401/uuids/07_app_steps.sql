SET search_path = public, pg_catalog;

--
-- Updates app_steps uuid foreign keys.
--
UPDATE workflow_io_maps SET source_step =
    (SELECT step.id FROM app_steps step
     LEFT JOIN transformation_steps ts ON ts.id = step.transformation_step_id
     WHERE source = ts.id);
UPDATE workflow_io_maps SET target_step =
    (SELECT step.id FROM app_steps step
     LEFT JOIN transformation_steps ts ON ts.id = step.transformation_step_id
     WHERE target = ts.id);


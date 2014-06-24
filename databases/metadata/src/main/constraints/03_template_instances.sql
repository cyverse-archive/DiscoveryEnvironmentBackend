SET search_path = public, pg_catalog;

--
-- template_instances table primary key.
--
ALTER TABLE template_instances
    ADD CONSTRAINT template_instances_pkey
    PRIMARY KEY (template_id, avu_id);

--
-- template_instances table foreign key to the avus table.
--
ALTER TABLE template_instances
    ADD CONSTRAINT template_instances_target_id_fkey
    FOREIGN KEY (avu_id)
    REFERENCES avus(id);


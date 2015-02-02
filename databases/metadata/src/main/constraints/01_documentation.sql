SET search_path = public, pg_catalog;

--
-- documentation table primary key.
--
ALTER TABLE documentation
ADD CONSTRAINT documentation_pkey
PRIMARY KEY (id);

--
-- documentation table unique values constraint.
--
ALTER TABLE documentation
ADD CONSTRAINT documentation_target_unique
UNIQUE (target_id, target_type);

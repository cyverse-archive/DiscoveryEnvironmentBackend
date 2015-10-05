SET search_path = public, pg_catalog;

--
-- Updates tool_architectures uuid foreign keys.
--
UPDATE tool_requests SET tool_architecture_id =
    (SELECT a.id FROM tool_architectures a
     WHERE a.id_v192 = tool_architecture_id_v192);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY tool_requests ALTER COLUMN tool_architecture_id SET NOT NULL;


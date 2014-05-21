SET search_path = public, pg_catalog;

--
-- Updates tool_architectures uuid foreign keys.
--
UPDATE tool_requests SET tool_architecture_id =
    (SELECT a.id FROM tool_architectures a
     WHERE a.id_v187 = tool_architecture_id_v187);


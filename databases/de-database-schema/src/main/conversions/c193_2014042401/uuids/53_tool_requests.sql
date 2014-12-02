SET search_path = public, pg_catalog;

--
-- Updates tool_requests uuid foreign keys.
--
UPDATE tool_request_statuses SET tool_request_id =
    (SELECT r.id FROM tool_requests r
     WHERE r.id_v192 = tool_request_id_v192);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY tool_request_statuses ALTER COLUMN tool_request_id SET NOT NULL;


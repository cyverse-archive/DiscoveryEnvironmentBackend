SET search_path = public, pg_catalog;

--
-- Updates tool_requests uuid foreign keys.
--
UPDATE tool_request_statuses SET tool_request_id =
    (SELECT r.id FROM tool_requests r
     WHERE r.id_v187 = tool_request_id_v187);


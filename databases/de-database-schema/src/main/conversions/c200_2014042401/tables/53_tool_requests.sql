SET search_path = public, pg_catalog;

--
-- Updates columns in the existing tool_requests table.
-- cols to drop: id_v187, requestor_id_v187, tool_architecture_id_v187, deployed_component_id
--
ALTER TABLE ONLY tool_requests RENAME COLUMN id TO id_v187;
ALTER TABLE ONLY tool_requests RENAME COLUMN uuid TO id;
ALTER TABLE ONLY tool_requests RENAME COLUMN requestor_id TO requestor_id_v187;
ALTER TABLE ONLY tool_requests ADD COLUMN requestor_id UUID;
ALTER TABLE ONLY tool_requests RENAME COLUMN tool_architecture_id TO tool_architecture_id_v187;
ALTER TABLE ONLY tool_requests ADD COLUMN tool_architecture_id UUID;
ALTER TABLE ONLY tool_requests ADD COLUMN tool_id UUID;


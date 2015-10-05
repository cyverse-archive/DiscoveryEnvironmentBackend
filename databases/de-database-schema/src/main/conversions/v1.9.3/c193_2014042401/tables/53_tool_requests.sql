SET search_path = public, pg_catalog;

--
-- Updates columns in the existing tool_requests table.
--
ALTER TABLE ONLY tool_requests RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY tool_requests RENAME COLUMN requestor_id TO requestor_id_v192;
ALTER TABLE ONLY tool_requests ALTER COLUMN requestor_id_v192 DROP NOT NULL;
ALTER TABLE ONLY tool_requests RENAME COLUMN tool_architecture_id TO tool_architecture_id_v192;
ALTER TABLE ONLY tool_requests ALTER COLUMN tool_architecture_id_v192 DROP NOT NULL;
ALTER TABLE ONLY tool_requests RENAME COLUMN deployed_component_id TO deployed_component_id_v192;
ALTER TABLE ONLY tool_requests RENAME COLUMN uuid TO id;
ALTER TABLE ONLY tool_requests ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY tool_requests ADD COLUMN requestor_id UUID;
ALTER TABLE ONLY tool_requests ADD COLUMN tool_architecture_id UUID;
ALTER TABLE ONLY tool_requests ADD COLUMN tool_id UUID;


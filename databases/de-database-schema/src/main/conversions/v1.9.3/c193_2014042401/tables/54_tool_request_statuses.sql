SET search_path = public, pg_catalog;

--
-- Updates columns in the existing tool_request_statuses table.
--
ALTER TABLE ONLY tool_request_statuses RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY tool_request_statuses RENAME COLUMN tool_request_id TO tool_request_id_v192;
ALTER TABLE ONLY tool_request_statuses RENAME COLUMN updater_id TO updater_id_v192;
ALTER TABLE ONLY tool_request_statuses ALTER COLUMN tool_request_id_v192 DROP NOT NULL;
ALTER TABLE ONLY tool_request_statuses ALTER COLUMN updater_id_v192 DROP NOT NULL;
ALTER TABLE ONLY tool_request_statuses ADD COLUMN id UUID DEFAULT (uuid_generate_v1());
ALTER TABLE ONLY tool_request_statuses ADD COLUMN tool_request_id UUID;
ALTER TABLE ONLY tool_request_statuses ADD COLUMN updater_id UUID;


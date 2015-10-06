SET search_path = public, pg_catalog;

--
-- Updates columns in the existing tool_request_status_codes table.
--
ALTER TABLE ONLY tool_request_status_codes ALTER COLUMN id SET DEFAULT uuid_generate_v1();
DROP SEQUENCE tool_request_status_codes_id_seq;
ALTER TABLE ONLY tool_request_status_codes ALTER COLUMN description TYPE TEXT;


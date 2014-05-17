SET search_path = public, pg_catalog;

--
-- Updates columns in the existing info_type table.
-- cols to drop: hid
--
ALTER TABLE ONLY info_type ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);


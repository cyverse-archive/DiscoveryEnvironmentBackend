SET search_path = public, pg_catalog;

--
-- Updates columns in the existing value_type table.
-- cols to drop: hid
--
ALTER TABLE ONLY value_type ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);


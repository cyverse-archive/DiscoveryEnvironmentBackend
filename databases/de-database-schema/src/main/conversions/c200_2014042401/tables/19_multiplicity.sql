SET search_path = public, pg_catalog;

--
-- Updates columns in the existing multiplicity table.
-- cols to drop: hid
--
ALTER TABLE ONLY multiplicity ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);


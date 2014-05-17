SET search_path = public, pg_catalog;

--
-- Updates columns in the existing rule_subtype table.
-- cols to drop: hid
--
ALTER TABLE ONLY rule_subtype ALTER COLUMN id TYPE UUID USING CAST(id AS UUID);


SET search_path = public, pg_catalog;

--
-- Updates columns in the existing version table.
--
ALTER TABLE ONLY version DROP COLUMN id;
DROP SEQUENCE version_id_seq;


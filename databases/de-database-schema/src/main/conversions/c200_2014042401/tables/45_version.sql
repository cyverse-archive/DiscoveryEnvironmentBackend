SET search_path = public, pg_catalog;

--
-- Updates columns in the existing version table.
--
ALTER TABLE ONLY version DROP COLUMN id;
ALTER TABLE ONLY version ADD PRIMARY KEY (version);


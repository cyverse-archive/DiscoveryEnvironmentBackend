SET search_path = public, pg_catalog;

--
-- Updates columns in the existing jobs table.
--
ALTER TABLE ONLY jobs RENAME COLUMN user_id TO user_id_v192;
ALTER TABLE ONLY jobs ALTER COLUMN user_id_v192 DROP NOT NULL;
ALTER TABLE ONLY jobs ALTER COLUMN id SET DEFAULT uuid_generate_v1();
ALTER TABLE ONLY jobs ADD COLUMN user_id UUID;
ALTER TABLE ONLY jobs ADD COLUMN parent_id UUID;
ALTER TABLE ONLY jobs ADD COLUMN notify boolean DEFAULT FALSE NOT NULL;

--
-- Mark all existing jobs as having notifications enabled to match the current functionality.
--
UPDATE jobs SET notify = TRUE;

SET search_path = public, pg_catalog;

--
-- Updates jobs foreign pseudo-keys into the apps table. These aren't real
-- actually foreign keys because the app ID can refer to either a DE app,
-- which will be stored in the apps table, or an Agave app, which will not.
-- Note that the type of the app_id column in the jobs table is not changing.
--
UPDATE jobs j
SET app_id = CAST(a.id AS character varying)
FROM apps a
WHERE j.app_id = a.id_v192;

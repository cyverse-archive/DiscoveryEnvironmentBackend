SET search_path = public, pg_catalog;

--
-- transformation_activity_mappings table
--
CREATE TABLE transformation_activity_mappings (
    app_id character varying(255) NOT NULL,
    mapping_id bigint NOT NULL,
    hid integer NOT NULL
);

SET search_path = public, pg_catalog;

--
-- property_group_property table
--
CREATE TABLE property_group_property (
    property_group_id bigint NOT NULL,
    property_id bigint NOT NULL,
    hid integer NOT NULL
);

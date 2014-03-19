SET search_path = public, pg_catalog;

--
-- template_property_group table
--
CREATE TABLE template_property_group (
    template_id bigint NOT NULL,
    property_group_id bigint NOT NULL,
    hid integer NOT NULL
);

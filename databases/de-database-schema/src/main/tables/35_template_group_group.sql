SET search_path = public, pg_catalog;

--
-- template_group_group table
--
CREATE TABLE template_group_group (
    parent_group_id bigint NOT NULL,
    subgroup_id bigint NOT NULL,
    hid integer NOT NULL
);

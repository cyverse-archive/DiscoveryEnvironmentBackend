SET search_path = public, pg_catalog;

--
-- template_group_group table
--
CREATE TABLE template_group_group (
    parent_category_id character varying(255) NOT NULL,
    child_category_id character varying(255) NOT NULL,
    hid integer NOT NULL
);

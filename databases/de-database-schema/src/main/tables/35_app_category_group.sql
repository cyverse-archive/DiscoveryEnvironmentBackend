SET search_path = public, pg_catalog;

--
-- app_category_group table
--
CREATE TABLE app_category_group (
    parent_category_id character varying(255) NOT NULL,
    child_category_id character varying(255) NOT NULL,
    hid integer NOT NULL
);

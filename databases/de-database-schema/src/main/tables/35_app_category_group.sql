SET search_path = public, pg_catalog;

--
-- app_category_group table
--
CREATE TABLE app_category_group (
    parent_category_id uuid NOT NULL,
    child_category_id uuid NOT NULL,
    child_index integer NOT NULL
);

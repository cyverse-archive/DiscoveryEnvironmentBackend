SET search_path = public, pg_catalog;

--
-- app_category_app table
--
CREATE TABLE app_category_app (
    app_category_id uuid NOT NULL,
    app_id uuid NOT NULL
);

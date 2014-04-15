SET search_path = public, pg_catalog;

--
-- app_references table
--
CREATE TABLE app_references (
    id uuid NOT NULL,
    app_id uuid NOT NULL,
    reference_text text NOT NULL
);

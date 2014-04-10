SET search_path = public, pg_catalog;

--
-- app_references table
--
CREATE TABLE app_references (
    id character varying(255) NOT NULL,
    app_id character varying(255) NOT NULL,
    reference_text text NOT NULL
);

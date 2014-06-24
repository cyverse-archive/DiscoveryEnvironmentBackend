SET search_path = public, pg_catalog;

--
-- workspace table
--
CREATE TABLE workspace (
    id uuid NOT NULL DEFAULT uuid_generate_v1(),
    root_category_id uuid NOT NULL,
    is_public boolean DEFAULT false,
    user_id uuid NOT NULL
);


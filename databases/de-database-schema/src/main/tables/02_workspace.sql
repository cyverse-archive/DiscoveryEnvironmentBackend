SET search_path = public, pg_catalog;

--
-- workspace table
--
CREATE TABLE workspace (
    id uuid NOT NULL,
    root_category_id uuid NOT NULL,
    is_public boolean DEFAULT false,
    user_id uuid NOT NULL
);


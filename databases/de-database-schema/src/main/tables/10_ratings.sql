SET search_path = public, pg_catalog;

--
-- ratings table
--
CREATE TABLE ratings (
    id uuid NOT NULL,
    user_id uuid NOT NULL,
    app_id uuid NOT NULL,
    rating integer NOT NULL,
    comment_id uuid
);

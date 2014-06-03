SET search_path = public, pg_catalog;

--
-- comments table
--
CREATE TABLE comments (
  id UUID NOT NULL,
  value TEXT,
  post_time timestamp DEFAULT now() NOT NULL,
  deleted BOOLEAN DEFAULT false,
  target_id UUID NOT NULL,
  owner_id UUID NOT NULL
);


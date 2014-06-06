SET search_path = public, pg_catalog;

--
-- comments table
--
CREATE TABLE comments (
  id UUID NOT NULL DEFAULT uuid_generate_v1(),
  value TEXT,
  post_time timestamp DEFAULT now() NOT NULL,
  deleted BOOLEAN DEFAULT false,
  target_id UUID NOT NULL,
  owner_id varchar(512) NOT NULL
);

CREATE INDEX comments_target_id_idx ON comments(target_id);


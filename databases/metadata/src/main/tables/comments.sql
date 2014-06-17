SET search_path = public, pg_catalog;

--
-- comments table
--
CREATE TABLE comments (
  id UUID NOT NULL DEFAULT uuid_generate_v1(),
  value TEXT NOT NULL,
  post_time timestamp DEFAULT now() NOT NULL,
  retracted BOOLEAN NOT NULL DEFAULT false,
  retracted_by varchar(512) DEFAULT NULL,
  deleted BOOLEAN NOT NULL DEFAULT false,
  target_id UUID NOT NULL,
  target_type target_enum NOT NULL,
  owner_id varchar(512) NOT NULL
);

CREATE INDEX comments_target_id_idx ON comments(target_id);


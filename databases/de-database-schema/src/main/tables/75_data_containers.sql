SET search_path = public, pg_catalog;

---
--- Tracks Docker data containers used with tools.
---
CREATE TABLE data_containers (
  -- Primary key
  id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v1(),

  -- The prefix part of the container name. The rest will consist of the
  -- analysis/job ID.
  name_prefix TEXT NOT NULL,

  -- Foreign key into the container_images table
  container_images_id UUID NOT NULL,

  -- Whether the container is mounted read-only
  read_only BOOLEAN NOT NULL DEFAULT TRUE
)

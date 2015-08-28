SET search_path = public, pg_catalog;

---
--- Tracks Docker data containers used with tools.
---
CREATE TABLE data_containers (
  -- Primary key
  id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),

  -- The prefix part of the container name. The rest will consist of the
  -- analysis/job ID.
  name_prefix TEXT NOT NULL,

  -- Foreign key into the container_images table
  container_image_id UUID NOT NULL,

  -- Whether the container is mounted read-only
  read_only BOOLEAN NOT NULL DEFAULT false,

  -- Don't allow a data container to get listed with the same container_image_id
  -- more than once.
  unique(id, container_image_id)
)

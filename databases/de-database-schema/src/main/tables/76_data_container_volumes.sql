SET search_path = public, pg_catalog;

--
-- Tracks the volumes associated with a data container
--
CREATE TABLE data_container_volumes (
  -- Primary key
  id UUID UNIQUE NOT NULL DEFAULT uuid_generate_v4(),

  -- Foreign key into the data_containers table
  data_container_id UUID NOT NULL,

  -- In-container path to the volumes
  volume_container_path TEXT NOT NULL,

  -- Host path (if any) to the volume. Can be null since it isn't required
  volume_host_path TEXT,

  -- The same volume shouldn't be associated with a single data container more
  -- than once.
  unique(id, data_container_id)
)

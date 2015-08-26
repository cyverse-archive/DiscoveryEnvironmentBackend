SET search_path = public, pg_catalog;

--
-- Contains records of which containers to mount volumes from. This is
-- associated with a container_settings record since this is done on a per-tool
-- basis rather than on a per-image basis.
--
CREATE TABLE container_volumes_from (
  -- primary key
  id uuid NOT NULL UNIQUE DEFAULT uuid_generate_v1(),

  -- foreign key into the data_containers table.
  data_containers_id uuid NOT NULL,

  -- foreign key into the container_settings_table.
  container_settings_id uuid NOT NULL
)

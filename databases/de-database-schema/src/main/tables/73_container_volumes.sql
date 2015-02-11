SET search_path = public, pg_catalog;

--
-- Records the volumes that need to get mounted into the container from the
-- host. This is associated with a container_settings record since it's used on
-- a per-tool basis.
CREATE TABLE container_volumes (
  -- primary key
  id uuid UNIQUE NOT NULL DEFAULT uuid_generate_v1(),

  -- foreign key into the container_settings table
  container_settings_id uuid NOT NULL,

  -- The path to the directory or file on the host that will be mounted into
  -- the container.
  host_path text NOT NULL,

  -- The path in the container that the host_path will be mounted to.
  container_path text NOT NULL
);

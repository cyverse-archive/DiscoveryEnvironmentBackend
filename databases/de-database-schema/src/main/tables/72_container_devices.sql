SET search_path = public, pg_catalog;

--
-- Contains the devices that need to get bind mounted into a container. The
-- container_devices are associated with a record in container_settings since
-- they are tool specific, not image specific.
--
CREATE TABLE container_devices(
  -- primary key
  id uuid NOT NULL,

  -- The foreign key into the container_settings table.
  container_settings_id uuid NOT NULL,

  -- The path on the host machine to the device to mount.
  host_path text NOT NULL,

  -- The path in the container to map the device to.
  container_path text NOT NULL
)

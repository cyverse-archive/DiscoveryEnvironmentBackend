SET search_path = public, pg_catalog;

--
-- Records the settings to use with a tool's container. This is stored on a per
-- tool basis rather than on a per-image basis since a single image can contain
-- multiple tools, each of which have different setting requirements.
--
CREATE TABLE container_settings (
  -- Primary key
  id uuid UNIQUE NOT NULL,

  -- The 'shares' of the CPU that the container owns.
  cpu_shares integer,

  -- The number of bytes to limit RAM to. integer isn't big enough, though
  -- bigint is overkill.
  memory_limit bigint,

  -- Most likely going to be 'bridge' or 'none', but may be set to a container
  -- id which is why we're using text.
  network_mode text,

  -- The directory the job will default to working in. Will most likely get set
  -- to null, indicating that we should just use the default. The data type is
  -- text to avoid annoying path length issues.
  working_directory text,

  -- The name of the running container. Most of the time this will be null, but
  -- we should have the option if we need it. Note that this is different from
  -- the image name.
  name text
);

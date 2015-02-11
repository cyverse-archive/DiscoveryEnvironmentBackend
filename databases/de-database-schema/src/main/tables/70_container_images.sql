SET search_path = public, pg_catalog;

--
-- Contains details about container images used as tools
--
CREATE TABLE container_images (
  id uuid UNIQUE  NOT NULL,   -- primary key
  name text NOT NULL, -- name used to indicate which image to pull down. Could be a UUID, but don't do that.
  tag text NOT NULL,  -- tag used to pull down an image. We'll default it to 'latest'
  url text            -- URL containing more information about the image (ex: docker hub URL)
);

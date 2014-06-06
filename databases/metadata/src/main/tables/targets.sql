SET search_path = public, pg_catalog;


--
-- target type enumeration
-- Currently, only analyses, apps, data and users may be targets of metadata
--
CREATE TYPE target_type AS ENUM ('analysis', 'app', 'data', 'user');

--
-- targets table
--
CREATE TABLE targets (
  id UUID NOT NULL,
  type target_type NOT NULL
);


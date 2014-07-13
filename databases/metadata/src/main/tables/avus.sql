SET search_path = public, pg_catalog;

--
-- avus table
--
CREATE TABLE avus (
  id UUID NOT NULL DEFAULT uuid_generate_v1(),
  attribute TEXT,
  value TEXT,
  unit TEXT,
  target_id UUID NOT NULL,
  target_type target_enum NOT NULL,
  created_by varchar(512) NOT NULL,
  modified_by varchar(512) NOT NULL,
  created_on timestamp DEFAULT now() NOT NULL,
  modified_on timestamp DEFAULT now() NOT NULL
);

CREATE INDEX avus_target_id_idx ON avus(target_id, target_type);
CREATE INDEX avus_avu_idx ON avus(attribute, value, unit);


SET search_path = public, pg_catalog;

--
-- attr_enum_values table primary key.
--
ALTER TABLE attr_enum_values
ADD CONSTRAINT attr_enum_values_pkey
PRIMARY KEY (id);

--
-- Foreign key constraint for the attribute_id field of the attr_enum_values table.
--
ALTER TABLE ONLY attr_enum_values
ADD CONSTRAINT attr_enum_values_attribute_id_fkey
FOREIGN KEY (attribute_id)
REFERENCES attributes(id) ON DELETE CASCADE;

--
-- Add a uniqueness constraint for each attribute's enum value.
--
ALTER TABLE ONLY attr_enum_values
ADD CONSTRAINT attr_enum_values_unique
UNIQUE(attribute_id, value);

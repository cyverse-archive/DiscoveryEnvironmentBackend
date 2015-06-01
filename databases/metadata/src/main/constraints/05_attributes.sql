--
-- Primary Key for the attributes table.
--
ALTER TABLE ONLY attributes
ADD CONSTRAINT attributes_pkey
PRIMARY KEY (id);

--
-- Foreign key constraint for the value_type_id field of the attributes table.
--
ALTER TABLE ONLY attributes
ADD CONSTRAINT attributes_value_type_id_fkey
FOREIGN KEY (value_type_id)
REFERENCES value_types(id);

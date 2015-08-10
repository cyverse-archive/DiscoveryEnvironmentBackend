SET search_path = public, pg_catalog;


--
-- value_type names should be unique (and, incidentally, indexed)
--
ALTER TABLE value_types
    ADD CONSTRAINT value_types_unique_name
    UNIQUE (name);

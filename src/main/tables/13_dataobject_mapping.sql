SET search_path = public, pg_catalog;

--
-- dataobject_mapping table
--
CREATE TABLE dataobject_mapping (
    mapping_id bigint NOT NULL,
    input character varying(255) NOT NULL,
    output character varying(255) NOT NULL
);

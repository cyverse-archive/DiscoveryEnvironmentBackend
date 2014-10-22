SET search_path = public, pg_catalog;

--
-- input_output_mapping table
--
CREATE TABLE input_output_mapping (
    mapping_id uuid NOT NULL,
    input uuid,
    external_input character varying(255),
    output uuid,
    external_output character varying(255)
);

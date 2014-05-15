SET search_path = public, pg_catalog;

--
-- input_output_mapping table
--
CREATE TABLE input_output_mapping (
    mapping_id uuid NOT NULL,
    input uuid NOT NULL,
    output uuid NOT NULL
);

SET search_path = public, pg_catalog;

--
-- Associate rule types with value types.
--
CREATE TABLE rule_type_value_type (
    rule_type_id uuid NOT NULL,
    value_type_id uuid NOT NULL
);

SET search_path = public, pg_catalog;

--
-- rule_argument table
--
CREATE TABLE rule_argument (
    rule_id bigint NOT NULL,
    argument_value text,
    hid integer NOT NULL
);

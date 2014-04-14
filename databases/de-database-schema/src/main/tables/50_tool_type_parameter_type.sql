SET search_path = public, pg_catalog;

--
-- A table indicating which parameter types can be used for which deployed
-- components.
--
CREATE TABLE tool_type_parameter_type (
   tool_type_id bigint NOT NULL,
   property_type_id bigint NOT NULL
);

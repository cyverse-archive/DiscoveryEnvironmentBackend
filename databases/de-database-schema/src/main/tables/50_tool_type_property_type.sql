SET search_path = public, pg_catalog;

--
-- A table indicating which property types can be used for which deployed
-- components.
--
CREATE TABLE tool_type_property_type (
   tool_type_id bigint NOT NULL,
   property_type_id bigint NOT NULL
);

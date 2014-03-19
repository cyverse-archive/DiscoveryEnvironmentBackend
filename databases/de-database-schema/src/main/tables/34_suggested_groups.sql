SET search_path = public, pg_catalog;

--
-- A table to store a list of groups that a user would like their public App to
-- be categorized in by the admins.
--
CREATE TABLE suggested_groups (
    transformation_activity_id bigint NOT NULL,
    template_group_id bigint NOT NULL
);

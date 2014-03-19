SET search_path = public, pg_catalog;

--
-- An ID counter for the collaborators table.
--
CREATE SEQUENCE collaborators_id_seq;

--
-- Collaborators
--
CREATE TABLE collaborators (
    id bigint DEFAULT nextval('collaborators_id_seq'),
    user_id bigint NOT NULL,
    collaborator_id bigint NOT NULL
);

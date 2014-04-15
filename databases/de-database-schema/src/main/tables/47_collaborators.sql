SET search_path = public, pg_catalog;

--
-- Collaborators
--
CREATE TABLE collaborators (
    user_id uuid NOT NULL,
    collaborator_id uuid NOT NULL
);

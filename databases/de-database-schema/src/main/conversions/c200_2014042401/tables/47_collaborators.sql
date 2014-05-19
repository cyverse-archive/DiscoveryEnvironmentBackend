SET search_path = public, pg_catalog;

--
-- Updates columns in the existing collaborators table.
-- cols to drop: id_v187, user_id_v187, collaborator_id_v187
--
ALTER TABLE ONLY collaborators RENAME COLUMN id TO id_v187;
ALTER TABLE ONLY collaborators RENAME COLUMN user_id TO user_id_v187;
ALTER TABLE ONLY collaborators ADD COLUMN user_id UUID;
ALTER TABLE ONLY collaborators RENAME COLUMN collaborator_id TO collaborator_id_v187;
ALTER TABLE ONLY collaborators ADD COLUMN collaborator_id UUID;


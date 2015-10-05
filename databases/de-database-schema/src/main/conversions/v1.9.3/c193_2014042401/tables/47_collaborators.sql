SET search_path = public, pg_catalog;

--
-- Updates columns in the existing collaborators table.
--
DELETE FROM collaborators WHERE id IN (
    SELECT id FROM collaborators AS c
    JOIN (
        SELECT MIN(id) AS min_id,
               COUNT(*) AS count,
               user_id,
               collaborator_id
          FROM collaborators
      GROUP BY user_id, collaborator_id
    ) AS n ON n.user_id = c.user_id AND n.collaborator_id = c.collaborator_id
    WHERE count > 1
      AND c.id > n.min_id
);

ALTER TABLE ONLY collaborators RENAME COLUMN id TO id_v192;
ALTER TABLE ONLY collaborators RENAME COLUMN user_id TO user_id_v192;
ALTER TABLE ONLY collaborators RENAME COLUMN collaborator_id TO collaborator_id_v192;
ALTER TABLE ONLY collaborators ALTER COLUMN user_id_v192 DROP NOT NULL;
ALTER TABLE ONLY collaborators ALTER COLUMN collaborator_id_v192 DROP NOT NULL;
ALTER TABLE ONLY collaborators ADD COLUMN user_id UUID;
ALTER TABLE ONLY collaborators ADD COLUMN collaborator_id UUID;


SET search_path = public, pg_catalog;

--
-- Updates users uuid foreign keys.
--
UPDATE workspace SET user_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = user_id_v192);

UPDATE ratings SET user_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = user_id_v192);

UPDATE genome_reference SET created_by =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = created_by_v192);

UPDATE genome_reference SET last_modified_by =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = last_modified_by_v192);

UPDATE collaborators SET user_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = user_id_v192);

UPDATE collaborators SET collaborator_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = collaborator_id_v192);

UPDATE tool_requests SET requestor_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = requestor_id_v192);

UPDATE tool_request_statuses SET updater_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = updater_id_v192);

UPDATE logins SET user_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = user_id_v192);

UPDATE jobs SET user_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = user_id_v192);

UPDATE user_preferences SET user_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = user_id_v192);

UPDATE user_sessions SET user_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = user_id_v192);

UPDATE user_saved_searches SET user_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = user_id_v192);

UPDATE access_tokens SET user_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = user_id_v192);

UPDATE authorization_requests SET user_id =
    (SELECT u.id FROM users u
     WHERE u.id_v192 = user_id_v192);

-- Add NOT NULL constraints on foreign key columns.
ALTER TABLE ONLY workspace ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ONLY ratings ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ONLY genome_reference ALTER COLUMN created_by SET NOT NULL;
ALTER TABLE ONLY genome_reference ALTER COLUMN last_modified_by SET NOT NULL;
ALTER TABLE ONLY collaborators ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ONLY collaborators ALTER COLUMN collaborator_id SET NOT NULL;
ALTER TABLE ONLY tool_requests ALTER COLUMN requestor_id SET NOT NULL;
ALTER TABLE ONLY tool_request_statuses ALTER COLUMN updater_id SET NOT NULL;
ALTER TABLE ONLY logins ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ONLY jobs ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ONLY user_preferences ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ONLY user_sessions ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ONLY user_saved_searches ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ONLY access_tokens ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE ONLY authorization_requests ALTER COLUMN user_id SET NOT NULL;


INSERT INTO workspace (id, is_public, user_id)
       SELECT '00000000-0000-0000-0000-000000000000', TRUE, u.id
       FROM users u
       WHERE u.username = '<public>';


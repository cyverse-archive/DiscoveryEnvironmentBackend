INSERT INTO workspace (id, root_category_id, is_public, user_id)
       SELECT '00000000-0000-0000-0000-000000000000', ac.id, TRUE, u.id
       FROM app_categories ac, users u
       WHERE u.username = '<public>'
       AND ac.id = '12c7a585-ec23-3352-e313-02e323112a7c';

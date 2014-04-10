INSERT INTO workspace (id, root_category_id, is_public, user_id)
       SELECT 0, ac.id, TRUE, u.id
       FROM app_categories ac, users u
       WHERE u.username = '<public>'
       AND ac.id = 'g12c7a585ec233352e31302e323112a7ccf18bfd7364';

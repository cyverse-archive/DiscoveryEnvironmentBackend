INSERT INTO workspace (id, root_analysis_group_id, is_public, user_id)
       SELECT 0, tg.hid, TRUE, u.id
       FROM template_group tg, users u
       WHERE u.username = '<public>'
       AND tg.id = 'g12c7a585ec233352e31302e323112a7ccf18bfd7364';

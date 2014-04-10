INSERT INTO app_categories (id, name, description, workspace_id)
       VALUES ('g12c7a585ec233352e31302e323112a7ccf18bfd7364',
               'Public Apps', '', 0);

INSERT INTO app_categories (id, name, description, workspace_id)
       VALUES ('g5401bd146c144470aedd57b47ea1b979',
               'Beta', '', 0);

INSERT INTO app_category_group (parent_category_id, child_category_id, hid)
       SELECT parent.id, child.id, 0
       FROM app_categories parent, app_categories child
       WHERE parent.id = 'g12c7a585ec233352e31302e323112a7ccf18bfd7364'
       AND child.id = 'g5401bd146c144470aedd57b47ea1b979';

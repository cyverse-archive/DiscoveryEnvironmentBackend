INSERT INTO app_categories (id, name, description, workspace_id)
       VALUES ('12c7a585-ec23-3352-e313-02e323112a7c',
               'Public Apps', '', '00000000-0000-0000-0000-000000000000');

INSERT INTO app_categories (id, name, description, workspace_id)
       VALUES ('5401bd146c144470aedd57b47ea1b979',
               'Beta', '', '00000000-0000-0000-0000-000000000000');

INSERT INTO app_category_group (parent_category_id, child_category_id)
       SELECT parent.id, child.id
       FROM app_categories parent, app_categories child
       WHERE parent.id = '12c7a585-ec23-3352-e313-02e323112a7c'
       AND child.id = '5401bd146c144470aedd57b47ea1b979';

UPDATE workspace SET root_category_id = '12c7a585-ec23-3352-e313-02e323112a7c'
       WHERE id = '00000000-0000-0000-0000-000000000000';


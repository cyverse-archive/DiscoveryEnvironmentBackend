SET search_path = public, pg_catalog;


--
-- attached_tags table foreign key to the targets table.
--
ALTER TABLE attached_tags
ADD CONSTRAINT attached_tags_target_id_fkey
FOREIGN KEY (target_id)
REFERENCES targets(id);

--
-- attached_tags table foreign key to the tags table.
--
ALTER TABLE attached_tags
ADD CONSTRAINT attached_tags_tag_id_fkey
FOREIGN KEY (tag_id)
REFERENCES tags(id);

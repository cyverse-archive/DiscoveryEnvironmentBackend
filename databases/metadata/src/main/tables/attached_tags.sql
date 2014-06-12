SET search_path = public, pg_catalog;

--
-- attached_tags table
-- This table indicates that a certain tag is attached to a certain resource or target.
--
CREATE TABLE attached_tags (
  target_id UUID NOT NULL,
  target_type target_enum NOT NULL,
  tag_id UUID NOT NULL,
  attacher_id VARCHAR (512),
  attached_on TIMESTAMP NOT NULL DEFAULT now(),
  detacher_id VARCHAR (512) DEFAULT NULL,
  detached_on TIMESTAMP DEFAULT NULL
);

CREATE INDEX attached_tags_target_id_idx ON attached_tags(target_id);
CREATE INDEX attached_tags_tag_id_idx ON attached_tags(tag_id);
SET search_path = public, pg_catalog;

--
-- Adds new parameter types to the database.
--
INSERT INTO parameter_types
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '8F6C59D1-CB29-45FD-834E-A42770C3FAA6', 'ReferenceGenome',
           'A reference genome to use for alignments.', NULL, FALSE,
           TRUE, '17', vt.id
    FROM value_type vt WHERE vt.name = 'Input';

INSERT INTO parameter_types
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'D55C28BF-9F00-44A6-9CED-3DB8A46B8B40', 'ReferenceSequence',
           'A reference sequence file to use for alignments.', NULL, FALSE,
           TRUE, '18', vt.id
    FROM value_type vt WHERE vt.name = 'Input';

INSERT INTO parameter_types
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'B728413E-69EB-435E-A6F5-D00CB1F43DAA', 'ReferenceAnnotation',
           'A reference annotation file to use for alignments.', NULL, FALSE,
           TRUE, '19', vt.id
    FROM value_type vt WHERE vt.name = 'Input';

INSERT INTO parameter_types
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '8031799A-5C4A-46D6-A984-21230CF4A04A', 'FileFolderInput',
           'An input that can accept either a file or a folder as an input.', NULL, FALSE,
           TRUE, '20', vt.id
    FROM value_type vt WHERE vt.name = 'Input';

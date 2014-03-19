INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    VALUES ('pt2cf37b0d-5463-4aef-98a2-4db63d2f3dbc', 'ClipperSelector', '',
            NULL, TRUE, FALSE, '999', NULL);

INSERT INTO property_type
     (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    VALUES ('ptbea4f078-6296-4511-834a-27b6bc3c88ab', 'Script', '', NULL,
            TRUE, FALSE, '999', NULL);

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    VALUES ('pt9935e153-5765-4c2e-a2bc-676f88b11267', 'Mode', '', NULL, TRUE,
            FALSE, '999', NULL);

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    VALUES ('pt553f6a79-329e-470b-b827-ebbf2d2811f1', 'BarcodeSelector',
            '', NULL, TRUE, FALSE, '999', NULL);

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    VALUES ('pt8d7dfb62-2ba4-4ad1-b38e-068318200d9b', 'TNRSFileSelector',
            '', NULL, TRUE, FALSE, '999', NULL);

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'ptc5b85c6b-381e-44f6-a568-186f1fe7f03d', 'Info',
           'Informative static text', NULL, TRUE, FALSE, '999', vt.hid
    FROM value_type vt WHERE vt.name = 'String';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'ptffeca61a-f1b9-43ba-b6ff-fa77bb34f396', 'Text',
           'A text box (no caption or number check)', NULL, FALSE, TRUE, '1', vt.hid
    FROM value_type vt WHERE vt.name = 'String';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'ptd2340f11-d260-41b4-93fd-c1d695bf6fef', 'Number',
           'A text box that checks for valid number input', NULL, TRUE, FALSE, '999',
           vt.hid
    FROM value_type vt WHERE vt.name = 'Number';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'ptf22ca553-856b-4253-b0f3-514701ed4567', 'QuotedText',
           'A text box that will add quotes for passing string to command line',
           NULL, TRUE, FALSE, '999', vt.hid
    FROM value_type vt WHERE vt.name = 'String';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'pt206a93d6-bac4-4925-89fe-39c073e85c47', 'Flag',
           'A checkbox for yes/no selection', NULL, FALSE, TRUE, '3', vt.hid
    FROM value_type vt WHERE vt.name = 'Boolean';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'ptbabc3c29-39c2-47b5-8576-f3741f9ae329', 'Selection',
           'A list for selecting a choice (can be text)', NULL, TRUE, FALSE, '999',
           vt.hid
    FROM value_type vt WHERE vt.name = 'String';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'pt7c71012b-158d-44fd-bda1-a5fb4d43bfd8', 'ValueSelection',
           'A list for selecting a value (numeric range)', NULL, TRUE, FALSE, '999',
           vt.hid
    FROM value_type vt WHERE vt.name = 'Number';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'F03DD9AC-B586-4FE1-A75B-3E2967BD0207', 'MultiLineText',
           'A multiline text box', '', FALSE, TRUE, '2', vt.hid
    FROM value_type vt WHERE vt.name = 'String';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'pt67bdfe81-361e-41fe-852a-35159e1e7bc5', 'XBasePairs',
           'A text box with caption (x=user specified number)', NULL, TRUE,
           FALSE, '999', vt.hid
    FROM value_type vt WHERE vt.name = 'Number';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '871AA217-2E6C-48E2-880A-EE7815E8F7F8', 'XBasePairsText',
           'A text box with caption (x=user specified text)', '', TRUE, FALSE, '999',
           vt.hid
    FROM value_type vt WHERE vt.name = 'String';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'C00ED92F-5399-490C-A6E5-AAD0E140D7FE', 'Input',
           'Input file or folder', NULL, TRUE, FALSE, '999', vt.hid
    FROM value_type vt WHERE vt.name = 'Input';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'F7007237-040D-4253-9323-CAA4F71E9795', 'Output',
           'Output file or folder', NULL, FALSE, TRUE, '999', vt.hid
    FROM value_type vt WHERE vt.name = 'Output';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'A024716E-1F18-4AF7-B59E-0745786D1B69', 'EnvironmentVariable',
           'An environment variable that is set before running a job', NULL,
           FALSE, TRUE, '9', vt.hid
    FROM value_type vt WHERE vt.name = 'EnvironmentVariable';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '548A55C2-53FE-40A5-AD38-033F79C8C0AB', 'TreeSelection',
           'A hierarchical list for selecting a choice', NULL,
           FALSE, FALSE, '10', vt.hid
    FROM value_type vt WHERE vt.name = 'String';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'C389D80A-F94E-4904-B6EF-BD658A18FC8A', 'Integer',
           'An integer value.', NULL, FALSE, TRUE, '4', vt.hid
    FROM value_type vt WHERE vt.name = 'Number';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '01250DB2-F8E9-4D9E-B82E-C4713DA84068', 'Double',
           'A real number value.', NULL, FALSE, TRUE, '5', vt.hid
    FROM value_type vt WHERE vt.name = 'Number';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'C529C00A-8B6F-4B73-80DA-C460C09722ED', 'TextSelection',
           'A list for selecting a textual value.', NULL, FALSE, FALSE, '6', vt.hid
    FROM value_type vt WHERE vt.name = 'String';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '0F4E0460-893B-4724-BC7C-D145575B9B73', 'IntegerSelection',
           'A list for selecting an integer value.', NULL, FALSE, FALSE, '7', vt.hid
    FROM value_type vt WHERE vt.name = 'Number';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'B8566277-C368-40E9-8B66-BC1C884CF69B', 'DoubleSelection',
           'A list for selecting a real number value.', NULL, FALSE, FALSE, '8', vt.hid
    FROM value_type vt WHERE vt.name = 'Number';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '3B3FAD4C-691B-44A8-BF34-D406F9052239', 'FileInput',
           'A control allowing for the selection of a single file.', NULL, FALSE,
           FALSE, '11', vt.hid
    FROM value_type vt WHERE vt.name = 'Input';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '9633FD4C-5FFC-4471-B531-2ECAAA683E26', 'FolderInput',
           'A control allowing for the selection of an entire folder.',
           NULL, FALSE, FALSE, '12', vt.hid
    FROM value_type vt WHERE vt.name = 'Input';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT 'FD5C9D3E-663D-469C-9455-5EE59621BF0E', 'MultiFileSelector',
           'A control allowing for the selection of multiple files.',
           NULL, FALSE, FALSE, '13', vt.hid
    FROM value_type vt WHERE vt.name = 'Input';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '0E2E3BE4-18A8-487B-BB27-C96A5A5A141F', 'FileOutput',
           'A single output file.', NULL, FALSE, TRUE, '14', vt.hid
    FROM value_type vt WHERE vt.name = 'Output';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '108011CA-1908-494E-B76F-83BB2BA696D7', 'FolderOutput',
           'A collection of output files in a single folder.', NULL, FALSE,
           TRUE, '15', vt.hid
    FROM value_type vt WHERE vt.name = 'Output';

INSERT INTO property_type
    (id, name, description, label, deprecated, hidable, display_order, value_type_id)
    SELECT '8EF87E50-460F-402A-B5C8-BFBB83211A54', 'MultiFileOutput',
           'Multiple output files matched by a glob pattern.', NULL, FALSE,
           TRUE, '16', vt.hid
    FROM value_type vt WHERE vt.name = 'Output';

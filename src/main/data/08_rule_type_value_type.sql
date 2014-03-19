INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'IntBelowField'
    AND vt.name = 'Number';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'IntAbove'
    AND vt.name = 'Number';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'IntRange'
    AND vt.name = 'Number';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'IntAboveField'
    AND vt.name = 'Number';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'MustContain'
    AND vt.name = 'Number';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'MustContain'
    AND vt.name = 'String';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'MustContain'
    AND vt.name = 'EnvironmentVariable';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'DoubleRange'
    AND vt.name = 'Number';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'IntBelow'
    AND vt.name = 'Number';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'DoubleAbove'
    AND vt.name = 'Number';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'DoubleBelow'
    AND vt.name = 'Number';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'Regex'
    AND vt.name = 'String';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'Regex'
    AND vt.name = 'EnvironmentVariable';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'CharacterLimit'
    AND vt.name = 'EnvironmentVariable';

INSERT INTO rule_type_value_type (rule_type_id, value_type_id)
    SELECT rt.hid, vt.hid
    FROM rule_type rt, value_type vt
    WHERE rt.name = 'CharacterLimit'
    AND vt.name = 'String';

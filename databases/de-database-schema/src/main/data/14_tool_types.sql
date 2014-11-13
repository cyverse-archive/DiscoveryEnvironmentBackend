-- Populates the tool_types table.

INSERT INTO tool_types (id, name, label, description)
    VALUES ( 'DE1DBE6A-A2BB-4219-986B-D878C6A9E3E4', 'executable', 'UA', 'Run at the University of Arizona' );

INSERT INTO tool_types (id, name, label, description)
    VALUES ( 'FA713BB8-2838-4B63-AB3A-265DBB1D719C', 'fAPI', 'TACC', 'Run at the Texas Advanced Computing Center' );

INSERT INTO tool_types (id, name, label, description, hidden)
    VALUES ( '01E14110-1420-4DE0-8A70-B0DD420F6A84', 'internal', 'Internal DE tools.', 'Tools used internally by the Discovery Environment.', true );

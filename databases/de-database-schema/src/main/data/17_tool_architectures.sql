-- Populates the tool_architectures table.

INSERT INTO tool_architectures ( id, name, description ) VALUES
    ( 'A8220BBA-63FE-4139-B6C3-5E22B43E8413', '32-bit Generic', '32-bit executables on an unspecified architecture.' ),
    ( 'EF254514-6D9F-4869-8FB8-A719262EFCA3', '64-bit Generic', '64-bit executables on an unspecified architecture.' ),
    ( '44DF2E72-36C0-4753-99F7-10AF851BAE8F', 'Others', 'Another specific architecture.' ),
    ( '6AF24F59-5DE7-4E43-A000-B8059DC80B0A', 'Don''t know', 'Used in cases where the user doesn''t know the architecture.' );

-- Populates the tool_architectures table.

INSERT INTO tool_architectures ( name, description ) VALUES
    ( '32-bit Generic', '32-bit executables on an unspecified architecture.' ),
    ( '64-bit Generic', '64-bit executables on an unspecified architecture.' ),
    ( 'Others', 'Another specific architecture.' ),
    ( 'Don''t know', 'Used in cases where the user doesn''t know the architecture.' );

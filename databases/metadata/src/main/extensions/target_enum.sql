--
-- target type enumeration
-- Currently, only analyses, apps, files, folders, and users may be targets of metadata.
--
CREATE TYPE target_enum AS ENUM ('analysis', 'app', 'file', 'folder', 'user');


--
-- target type enumeration
-- Currently, only analyses, apps, data and users may be targets of metadata
--
CREATE TYPE target_enum AS ENUM ('analysis', 'app', 'data', 'user');


SET search_path = public, pg_catalog;

---
--- A table for storing authorization request state information.
---
CREATE TABLE authorization_requests (
    id UUID NOT NULL,
    user_id BIGINT UNIQUE NOT NULL,
    state_info TEXT NOT NULL,
    PRIMARY KEY (id)
);

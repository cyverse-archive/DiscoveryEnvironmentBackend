SET search_path = public, pg_catalog;

---
--- A table for storing OAuth 2.0 authorization tokens.
---
CREATE TABLE authorization_tokens (
    id UUID NOT NULL,
    webapp_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL
);

SET search_path = public, pg_catalog;

---
--- A table for storing OAuth 2.0 access tokens.
---
CREATE TABLE access_tokens (
    webapp VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    token BYTEA NOT NULL,
    expires_at TIMESTAMP,
    refresh_token BYTEA
);

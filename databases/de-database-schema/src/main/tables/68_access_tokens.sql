SET search_path = public, pg_catalog;

---
--- A table for storing OAuth 2.0 authorization tokens.
---
CREATE TABLE access_tokens (
    webapp_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP,
    refresh_token VARCHAR(128)
);

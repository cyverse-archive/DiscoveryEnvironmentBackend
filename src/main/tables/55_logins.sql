SET search_path = public, pg_catalog;

--
-- A record of each login.
--
CREATE TABLE logins (
    user_id bigint,
    ip_address varchar(15) NOT NULL,
    user_agent text,
    login_time timestamp NOT NULL DEFAULT now(),
    logout_time timestamp
);

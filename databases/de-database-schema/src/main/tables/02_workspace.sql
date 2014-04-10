SET search_path = public, pg_catalog;

--
-- id SERIAL type for workspace table
--
CREATE SEQUENCE workspace_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


--
-- workspace table
--
CREATE TABLE workspace (
    id bigint DEFAULT nextval('workspace_id_seq'::regclass) NOT NULL,
    home_folder bigint,
    root_category_id character varying(255),
    is_public boolean DEFAULT false,
    user_id bigint
);


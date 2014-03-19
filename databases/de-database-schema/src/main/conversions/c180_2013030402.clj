(ns facepalm.c180-2013030402
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130304.02")

(defn- create-tool-request-status-codes-table
  "Creates the table that defines the available tool request status codes."
  []
  (println "\t* creating the table for tool request status codes")
  (exec-raw
   "CREATE SEQUENCE tool_request_status_codes_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MAXVALUE
        NO MINVALUE
        CACHE 1")
  (exec-raw
   "CREATE TABLE tool_request_status_codes (
        id BIGINT DEFAULT nextval('tool_request_status_codes_id_seq'::regclass) NOT NULL,
        name VARCHAR(16) NOT NULL,
        description VARCHAR(128) NOT NULL,
        PRIMARY KEY(id))")
  (exec-raw
   "CREATE UNIQUE INDEX tool_request_status_codes_name_index
        on tool_request_status_codes (name)"))

(defn- populate-tool-request-status-codes-table
  "Populates the tool_request_status_codes table with the set of available status codes."
  []
  (println "\t* populating the table for tool request status codes")
  (insert
   :tool_request_status_codes
   (values
    [{:name        "Submitted"
      :description "The request has been submitted, but not acted upon by the support team."}
     {:name        "Pending"
      :description "The support team is waiting for a response from the requesting user."}
     {:name        "Evaluation"
      :description "The support team is evaluating the tool for installation."}
     {:name        "Installation"
      :description "The support team is installing the tool."}
     {:name        "Validation"
      :description "The support team is verifying that the installation was successful."}
     {:name        "Completion"
      :description "The tool has been installed successfully."}
     {:name        "Failed"
      :description "The tool could not be installed."}])))

(defn- create-tool-architectures-table
  "Creates the table that defines the available tool architectures."
  []
  (println "\t* creating the table for tool architectures")
  (exec-raw
   "CREATE SEQUENCE tool_architectures_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MAXVALUE
        NO MINVALUE
        CACHE 1")
  (exec-raw
   "CREATE TABLE tool_architectures (
        id BIGINT DEFAULT nextval('tool_architectures_id_seq'::regclass) NOT NULL,
        name VARCHAR(64) NOT NULL,
        description VARCHAR(256) NOT NULL,
        PRIMARY KEY(id))")
  (exec-raw
   "CREATE UNIQUE INDEX tool_architectures_name_index
    ON tool_architectures (name)"))

(defn- populate-tool-architectures-table
  "Populates the tool_architectures table with the set of available architectures."
  []
  (println "\t* populating the table for available tool architectures")
  (insert
   :tool_architectures
   (values
    [{:name        "32-bit Generic"
      :description "32-bit executables on an unspecified architecture."}
     {:name        "64-bit Generic"
      :description "64-bit executables on an unspecified architecture."}])))

(defn- create-tool-requests-table
  "Creates the table that is used to track tool installation requests."
  []
  (println "\t* creating the table used to track tool installation requests")
  (exec-raw
   "CREATE SEQUENCE tool_requests_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MAXVALUE
        NO MINVALUE
        CACHE 1")
  (exec-raw
   "CREATE TABLE tool_requests (
        id BIGINT DEFAULT nextval('tool_requests_id_seq'::regclass) NOT NULL,
        uuid UUID NOT NULL,
        requestor_id BIGINT REFERENCES users(id) NOT NULL,
        phone VARCHAR(30),
        tool_name VARCHAR(255) NOT NULL,
        description TEXT NOT NULL,
        source_url TEXT NOT NULL,
        doc_url TEXT NOT NULL,
        version VARCHAR(64) NOT NULL,
        attribution TEXT NOT NULL,
        multithreaded BOOLEAN,
        tool_architecture_id BIGINT REFERENCES tool_architectures(id) NOT NULL,
        test_data_path TEXT NOT NULL,
        instructions TEXT NOT NULL,
        additional_info TEXT,
        additional_data_file TEXT,
        deployed_component_id BIGINT REFERENCES deployed_components(hid),
        PRIMARY KEY(id))"))

(defn- create-tool-request-statuses-table
  "Creates the table that is used to track tool request status updates."
  []
  (println "\t* creating the table that is used to track tool request status updates")
  (exec-raw
   "CREATE SEQUENCE tool_request_statuses_id_seq
        START WITH 1
        INCREMENT BY 1
        NO MAXVALUE
        NO MINVALUE
        CACHE 1")
  (exec-raw
   "CREATE TABLE tool_request_statuses (
        id BIGINT DEFAULT nextval('tool_request_statuses_id_seq'::regclass) NOT NULL,
        tool_request_id BIGINT REFERENCES tool_requests(id) NOT NULL,
        tool_request_status_code_id BIGINT REFERENCES tool_request_status_codes(id) NOT NULL,
        date_assigned TIMESTAMP DEFAULT now() NOT NULL,
        updater_id BIGINT NOT NULL REFERENCES users(id),
        comments TEXT,
        PRIMARY KEY(id))"))

(defn- create-first-aggregate-function
  "Creates an aggregate function that always returns the first non-null item."
  []
  (println "\t* creating the aggregate function, 'first'")
  (exec-raw
   "CREATE OR REPLACE FUNCTION public.first_agg ( anyelement, anyelement )
    RETURNS anyelement LANGUAGE sql IMMUTABLE STRICT AS $$
            SELECT $1
    $$")
  (exec-raw
   "CREATE AGGREGATE public.first (
        sfunc    = public.first_agg,
        basetype = anyelement,
        stype    = anyelement)"))

(defn- create-last-aggregate-function
  "Creates an aggregate function that always returns the last non-null item."
  []
  (println "\t* creating the aggregate function, 'last'")
  (exec-raw
   "CREATE OR REPLACE FUNCTION public.last_agg ( anyelement, anyelement )
    RETURNS anyelement LANGUAGE sql IMMUTABLE STRICT AS $$
        SELECT $2
    $$")
  (exec-raw
   "CREATE AGGREGATE public.last (
        sfunc    = public.last_agg,
        basetype = anyelement,
        stype    = anyelement)"))

(defn convert
  "Performs the database conversion for DE version 1.8.0:20130304.02."
  []
  (println "Performing conversion for" version)
  (create-tool-request-status-codes-table)
  (populate-tool-request-status-codes-table)
  (create-tool-architectures-table)
  (populate-tool-architectures-table)
  (create-tool-requests-table)
  (create-tool-request-statuses-table)
  (create-first-aggregate-function)
  (create-last-aggregate-function))

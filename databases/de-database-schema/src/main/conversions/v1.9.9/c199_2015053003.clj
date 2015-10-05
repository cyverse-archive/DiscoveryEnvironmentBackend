(ns facepalm.c199-2015053003
  (:use [korma.core]
        [kameleon.sql-reader :only [exec-sql-statement]]))

(def ^:private version
  "The destination database version."
  "1.9.9:20150530.03")

(defn- add-curl-wrapper-container
  []
  (println "\t* adding the curl-wrapper container")
  (exec-sql-statement
    "INSERT INTO container_images (id, \"name\", tag, url) VALUES
      ('15959300-b972-4571-ace2-081af0909599',
       'discoenv/curl-wrapper',
       'latest',
       'https://registry.hub.docker.com/u/discoenv/curl-wrapper/');")
  (exec-sql-statement
    "INSERT INTO container_settings (tools_id, network_mode) VALUES
      ('681251EF-EE59-4FE9-9436-DC8A23FEB11A', 'bridge');")
  (exec-sql-statement
    "UPDATE ONLY tools
        SET container_images_id = '15959300-b972-4571-ace2-081af0909599'
      WHERE id = '681251EF-EE59-4FE9-9436-DC8A23FEB11A';"))

(defn- add-notreal-internal-tool
  []
  (println "\t* adding the notreal internal tool for sanity checking and unit testing")
  (exec-sql-statement
    "INSERT INTO tools (id, \"name\", location, description, version, tool_type_id, integration_data_id)
        SELECT '4c7105ce-b900-405f-b067-cd3b152d3b4b',
               'notreal',
               '/not/real/',
               'not a real tool',
               '1.0.0',
               tool_types.id,
               integration_data.id
          FROM tool_types, integration_data
         WHERE tool_types.\"name\" = 'internal'
           AND integration_data.integrator_name = 'Internal DE Tools'
         LIMIT 1;"))

(defn convert
  "Performs the conversion for database version 1.9.9:20150530.03"
  []
  (println "Performing the conversion for" version)
  (add-curl-wrapper-container)
  (add-notreal-internal-tool))

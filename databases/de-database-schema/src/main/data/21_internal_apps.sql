SET search_path = public, pg_catalog;

--
-- The integration data information to be used for internal apps.
--
INSERT INTO integration_data (integrator_name, integrator_email)
    VALUES ('Internal DE Tools', 'support@iplantcollaborative.org');

--
-- The internal tool for the curl wrapper used for URL imports.
--
INSERT INTO tools (id, "name", location, description, version, tool_type_id, integration_data_id)
    SELECT '681251EF-EE59-4FE9-9436-DC8A23FEB11A',
           'curl_wrapper.pl',
           '/usr/local/bin',
           'curl wrapper for DE URL imports',
           '1.0.0',
           tool_types.id,
           integration_data.id
    FROM tool_types, integration_data
    WHERE tool_types."name" = 'internal'
    AND integration_data.integrator_name = 'Internal DE Tools'
    LIMIT 1;

--
-- A fake tool for testing.
--
INSERT INTO tools (id, "name", location, description, version, tool_type_id, integration_data_id)
    SELECT '4c7105ce-b900-405f-b067-cd3b152d3b4b',
           'notreal',
           '/not/real/',
           'not a real tool',
           '1.0.0',
           tool_types.id,
           integration_data.id
      FROM tool_types, integration_data
     WHERE tool_types."name" = 'internal'
       AND integration_data.integrator_name = 'Internal DE Tools'
     LIMIT 1;

--
-- The internal app for the curl wrapper used for URL imports.
--
INSERT INTO apps (id, "name", description, integration_data_id, wiki_url, integration_date)
    SELECT '1E8F719B-0452-4D39-A2F3-8714793EE3E6',
           'Curl Wrapper',
           'curl wrapper for DE URL imports',
           integration_data.id,
           '',
           now()
    FROM integration_data
    WHERE integrator_name = 'Internal DE Tools'
    LIMIT 1;

INSERT INTO tasks (id, "name", description, label, tool_id) VALUES
    ('212C5980-9A56-417E-A8C6-394AC445CA4D',
     'Curl Wrapper',
     'curl wrapper for DE URL imports',
     'Curl Wrapper',
     '681251EF-EE59-4FE9-9436-DC8A23FEB11A');

INSERT INTO parameter_groups (id, "name", description, label, task_id) VALUES
    ('30345113-D3E5-406B-A4E8-170A685E7A8B',
     'Parameters',
     'URL upload parameters',
     'Parameters',
     '212C5980-9A56-417E-A8C6-394AC445CA4D');

INSERT INTO parameters (id, "name", description, label, ordering, parameter_group_id,
                        parameter_type, display_order, required)
    SELECT '1DD009B1-CE1E-4933-ABA8-66314757288B',
           '-o',
           'The name of the uploaded file.',
           'Output Filename',
           0,
           '30345113-D3E5-406B-A4E8-170A685E7A8B',
           pt.id,
           0,
           TRUE
    FROM parameter_types pt
    WHERE pt."name" = 'FileOutput'
    LIMIT 1;

INSERT INTO file_parameters (id, parameter_id, info_type, data_format, data_source_id, retain)
    SELECT '75288DE6-323D-44CA-BEFA-8E14DAE109E4',
           '1DD009B1-CE1E-4933-ABA8-66314757288B',
           info_type.id,
           data_formats.id,
           data_source.id,
           TRUE
    FROM info_type, data_formats, data_source
    WHERE info_type."name" = 'File'
    AND data_formats."name" = 'Unspecified'
    AND data_source."name" = 'file'
    LIMIT 1;

INSERT INTO parameters (id, "name", description, label, ordering, parameter_group_id,
                        parameter_type, display_order, required)
    SELECT 'A0D6A102-8623-47B9-A57F-224D6A71F28D',
           '',
           'The URL to retrieve the file from.',
           'Source URL',
           1,
           '30345113-D3E5-406B-A4E8-170A685E7A8B',
           pt.id,
           1,
           TRUE
    FROM parameter_types pt
    WHERE pt."name" = 'Text'
    LIMIT 1;

INSERT INTO app_steps (step, id, app_id, task_id) VALUES
    (0,
     'EE78DEB5-EBBB-4D9D-8DCF-8DFE457A7856',
     '1E8F719B-0452-4D39-A2F3-8714793EE3E6',
     '212C5980-9A56-417E-A8C6-394AC445CA4D');

INSERT INTO container_images (name, tag, url) VALUES
    ('discoenv/curl-wrapper',
     'latest',
     'https://registry.hub.docker.com/u/discoenv/curl-wrapper/');

INSERT INTO container_settings (tools_id)
    SELECT tools.id
      FROM tools
     WHERE tools."name" = 'curl_wrapper.pl'
     LIMIT 1;

UPDATE ONLY tools SET container_images_id = (
  SELECT container_images.id
    FROM container_images
   WHERE container_images."name" = 'discoenv/curl-wrapper'
     AND container_images.tag = 'latest'
   LIMIT 1
);

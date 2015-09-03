SET search_path = public, pg_catalog;

--
-- The integration data information to be used for internal apps.
--
INSERT INTO integration_data (integrator_name, integrator_email)
    VALUES ('Internal DE Tools', 'support@iplantcollaborative.org');

--
-- The integration data information to be used for default DE apps.
--
INSERT INTO integration_data (integrator_name, integrator_email)
    VALUES ('Default DE Tools', 'support@iplantcollaborative.org');

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

--
-- New containerized word count tool
--
INSERT INTO tools (id, "name", location, description, version, tool_type_id, integration_data_id)
  SELECT '85cf7a33-386b-46fe-87c7-8c9d59972624',
         'wc',
         '',
         'Word Count',
         '0.0.1',
         tool_types.id,
         integration_data.id
    FROM tool_types, integration_data
   WHERE tool_types."name" = 'executable'
     AND integration_data.integrator_name = 'Default DE Tools'
   LIMIT 1;

--
-- The app for word count
--
INSERT INTO apps(id, "name", description, integration_data_id, wiki_url, integration_date)
   SELECT '67d15627-22c5-42bd-8daf-9af5deecceab',
          'DE Word Count',
          'Counts the number of words in a file',
          integration_data.id,
          '',
          now()
     FROM integration_data
    WHERE integrator_name = 'Default DE Tools'
    LIMIT 1;

INSERT INTO tasks (id, "name", description, label, tool_id) VALUES
    ('1ac31629-231a-4090-b3b4-63ee078a0c37',
     'DE Word Count',
     'Counts the number of words in a file',
     'DE Word Count',
     '85cf7a33-386b-46fe-87c7-8c9d59972624');

INSERT INTO parameter_groups (id, "name", description, label, task_id) VALUES
    ('741711b0-0b95-4ac9-98b4-ca58225e76be',
     'Parameters',
     'Word count parameters',
     'Parameters',
     '1ac31629-231a-4090-b3b4-63ee078a0c37');

INSERT INTO parameters (id, "name", description, label, ordering, parameter_group_id,
                     parameter_type, display_order, required)
  SELECT '13914010-89cd-406d-99c3-9c4ff8b023c3',
         '',
         'The file to count words in.',
         'Input Filename',
         0,
         '741711b0-0b95-4ac9-98b4-ca58225e76be',
         pt.id,
         0,
         TRUE
    FROM parameter_types pt
   WHERE pt."name" = 'FileInput'
   LIMIT 1;

INSERT INTO file_parameters (id, parameter_id, info_type, data_format, data_source_id, retain)
    SELECT 'a350604d-48a0-4083-b6b3-425f3b1f7f51',
           '13914010-89cd-406d-99c3-9c4ff8b023c3',
           info_type.id,
           data_formats.id,
           data_source.id,
           TRUE
      FROM info_type, data_formats, data_source
     WHERE info_type."name" = 'File'
       AND data_formats."name" = 'Unspecified'
       AND data_source."name" = 'file'
     LIMIT 1;

INSERT INTO app_category_app (app_category_id, app_id) VALUES
    ('5401bd146c144470aedd57b47ea1b979',
     '67d15627-22c5-42bd-8daf-9af5deecceab');

INSERT INTO app_steps (step, id, app_id, task_id) VALUES
    (0,
     '089a61a0-23d9-4021-9354-a8498ef3ff19',
     '67d15627-22c5-42bd-8daf-9af5deecceab',
     '1ac31629-231a-4090-b3b4-63ee078a0c37');

INSERT INTO container_images (id, "name", tag, url) VALUES
    ('15959300-b972-4571-ace2-081af0909599',
     'discoenv/curl-wrapper',
     'latest',
     'https://registry.hub.docker.com/u/discoenv/curl-wrapper/');

INSERT INTO container_images (id, "name", tag, url) VALUES
    ('fc210a84-f7cd-4067-939c-a68ec3e3bd2b',
     'gims.iplantcollaborative.org:5000/backwards-compat',
     'latest',
     'https://registry.hub.docker.com/u/discoenv/backwards-compat');

INSERT INTO container_settings (tools_id, network_mode)
  VALUES ('681251EF-EE59-4FE9-9436-DC8A23FEB11A', 'bridge');

INSERT INTO container_settings (tools_id, network_mode, entrypoint)
  VALUES ('85cf7a33-386b-46fe-87c7-8c9d59972624', 'none', 'wc');

-- Insert default container settings for tools that don't already have them.
INSERT INTO container_settings (tools_id)
  SELECT tools.id
    FROM tools
   WHERE tools.id NOT IN (
     SELECT container_settings.tools_id
       FROM container_settings
      WHERE container_settings.tools_id IS NOT NULL
   )
   AND tools.name != 'notreal';

INSERT INTO data_containers (id, name_prefix, container_images_id)
  VALUES ('115584ad-7bc3-4601-89a2-85a4e5b5f6a4', 'wc-data', '15959300-b972-4571-ace2-081af0909599');

INSERT INTO container_volumes_from (data_containers_id, container_settings_id)
  SELECT '115584ad-7bc3-4601-89a2-85a4e5b5f6a4',
         container_settings.id
    FROM container_settings
   WHERE container_settings.tools_id = '85cf7a33-386b-46fe-87c7-8c9d59972624'
     AND container_settings.network_mode = 'none'
     AND container_settings.entrypoint = 'wc'
   LIMIT 1;


UPDATE ONLY tools
   SET container_images_id = '15959300-b972-4571-ace2-081af0909599'
 WHERE id = '681251EF-EE59-4FE9-9436-DC8A23FEB11A';

-- The wc tool reuses the curl-wrapper image.
UPDATE ONLY tools
   SET container_images_id = '15959300-b972-4571-ace2-081af0909599'
 WHERE id = '85cf7a33-386b-46fe-87c7-8c9d59972624';

-- Everything else should use the discoenv/backwards-compat:latest container.
UPDATE ONLY tools
   SET container_images_id = 'fc210a84-f7cd-4067-939c-a68ec3e3bd2b'
 WHERE container_images_id IS NULL;

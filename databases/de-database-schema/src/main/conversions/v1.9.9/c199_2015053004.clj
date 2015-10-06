(ns facepalm.c199-2015053004
    (:use [korma.core]))

(def ^:private version
    "The destination database version."
    "1.9.9:20150530.04")

(defn- add-de-word-count-tool
  []
  (println "\t* Adding the word count tool to the database")
  (exec-raw
    "INSERT INTO integration_data (integrator_name, integrator_email)
          VALUES ('Default DE Tools', 'support@iplantcollaborative.org');")
  (exec-raw
      "INSERT INTO tools (id, \"name\", location, description, version, tool_type_id, integration_data_id)
        SELECT '85cf7a33-386b-46fe-87c7-8c9d59972624',
               'wc',
               '',
               'Word Count',
               '0.0.1',
               tool_types.id,
               integration_data.id
          FROM tool_types, integration_data
         WHERE tool_types.\"name\" = 'executable'
           AND integration_data.integrator_name = 'Default DE Tools'
         LIMIT 1;")
  (exec-raw
      "INSERT INTO apps(id, \"name\", description, integration_data_id, wiki_url, integration_date)
         SELECT '67d15627-22c5-42bd-8daf-9af5deecceab',
                'DE Word Count',
                'Counts the number of words in a file',
                integration_data.id,
                '',
                now()
           FROM integration_data
          WHERE integrator_name = 'Default DE Tools'
          LIMIT 1;")
  (exec-raw
      "INSERT INTO tasks (id, \"name\", description, label, tool_id) VALUES
          ('1ac31629-231a-4090-b3b4-63ee078a0c37',
           'DE Word Count',
           'Counts the number of words in a file',
           'DE Word Count',
           '85cf7a33-386b-46fe-87c7-8c9d59972624');")
  (exec-raw
      "INSERT INTO parameter_groups (id, \"name\", description, label, task_id) VALUES
          ('741711b0-0b95-4ac9-98b4-ca58225e76be',
           'Parameters',
           'Word count parameters',
           'Parameters',
           '1ac31629-231a-4090-b3b4-63ee078a0c37');")
  (exec-raw
      "INSERT INTO parameters (id, \"name\", description, label, ordering, parameter_group_id,
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
         WHERE pt.\"name\" = 'FileInput'
         LIMIT 1;")
  (exec-raw
      "INSERT INTO file_parameters (id, parameter_id, info_type, data_format, data_source_id, retain)
          SELECT 'a350604d-48a0-4083-b6b3-425f3b1f7f51',
                 '13914010-89cd-406d-99c3-9c4ff8b023c3',
                 info_type.id,
                 data_formats.id,
                 data_source.id,
                 TRUE
            FROM info_type, data_formats, data_source
           WHERE info_type.\"name\" = 'File'
             AND data_formats.\"name\" = 'Unspecified'
             AND data_source.\"name\" = 'file'
           LIMIT 1;")
  (exec-raw
      "INSERT INTO container_settings (tools_id) VALUES
         ('85cf7a33-386b-46fe-87c7-8c9d59972624');")
  (exec-raw
      "UPDATE ONLY tools
         SET container_images_id = '15959300-b972-4571-ace2-081af0909599'
       WHERE id = '85cf7a33-386b-46fe-87c7-8c9d59972624';")
  (exec-raw
    "INSERT INTO app_category_app (app_category_id, app_id) VALUES
        ('5401bd146c144470aedd57b47ea1b979',
         '67d15627-22c5-42bd-8daf-9af5deecceab');")
  (exec-raw
    "INSERT INTO app_steps (step, id, app_id, task_id) VALUES
        (0,
         '089a61a0-23d9-4021-9354-a8498ef3ff19',
         '67d15627-22c5-42bd-8daf-9af5deecceab',
         '1ac31629-231a-4090-b3b4-63ee078a0c37');"))

(defn convert
  []
  (println "Performing the conversion for" version)
  (add-de-word-count-tool))

SET search_path = public, pg_catalog;

--
-- A view of all parameters associated with a task.
--
CREATE VIEW task_param_listing AS
    SELECT t.id AS task_id,
           p.parameter_group_id,
           p.id,
           p.name,
           p.label,
           p.description,
           p.ordering,
           p.display_order,
           p.required,
           p.omit_if_blank,
           p.is_visible,
           pt.name AS parameter_type,
           vt.name AS value_type,
           f.retain,
           f.is_implicit,
           it.name AS info_type,
           df.name AS data_format,
           ds.name AS data_source
    FROM parameters p
        INNER JOIN parameter_types pt ON pt.id = p.parameter_type
        INNER JOIN value_type vt ON vt.id = pt.value_type_id
        LEFT JOIN file_parameters f ON f.parameter_id = p.id
        LEFT JOIN info_type it ON f.info_type = it.id
        LEFT JOIN data_formats df ON f.data_format = df.id
        LEFT JOIN data_source ds ON f.data_source_id = ds.id
        INNER JOIN parameter_groups g ON g.id = p.parameter_group_id
        INNER JOIN tasks t ON t.id = g.task_id;

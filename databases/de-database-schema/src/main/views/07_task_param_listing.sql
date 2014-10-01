SET search_path = public, pg_catalog;

--
-- A view of all parameters associated with a task.
--
CREATE VIEW task_param_listing AS
    SELECT t.id AS task_id,
           p.id,
           p.name,
           p.label,
           p.description,
           p.ordering,
           p.required,
           p.omit_if_blank,
           pt.name AS parameter_type,
           vt.name AS value_type,
           f.retain,
           f.is_implicit,
           f.info_type,
           f.data_format,
           f.data_source_id
    FROM parameters p
        LEFT JOIN parameter_types pt ON pt.id = p.parameter_type
        LEFT JOIN value_type vt ON vt.id = pt.value_type_id
        LEFT JOIN file_parameters f ON f.id = p.file_parameter_id
        LEFT JOIN parameter_groups g ON g.id = p.parameter_group_id
        LEFT JOIN tasks t ON t.id = g.task_id;

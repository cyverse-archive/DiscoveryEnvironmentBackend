SET search_path = public, pg_catalog;

--
-- A view containing the job information needed to produce a job listing.
--
CREATE VIEW job_listings AS
    SELECT j.id,
           j.job_name,
           j.app_name,
           j.start_date,
           j.end_date,
           j.status,
           j.deleted,
           j.notify,
           u.username,
           j.job_description,
           j.app_id,
           j.app_wiki_url,
           j.app_description,
           j.result_folder_path,
           j.submission,
           CASE WHEN COUNT(DISTINCT t.name) > 1 THEN 'DE'
                ELSE MAX(t.name)
           END AS job_type,
           j.parent_id,
                      EXISTS (
               SELECT * FROM jobs child
               WHERE child.parent_id = j.id
           ) AS is_batch
    FROM jobs j
         JOIN users u ON j.user_id = u.id
         JOIN job_steps s ON j.id = s.job_id
         JOIN job_types t ON s.job_type_id = t.id
    GROUP BY j.id, u.username;

(ns facepalm.c189-2014071801
  (:use [korma.core]
        [kameleon.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20140718.01")

(defn- add-job-listings-view
  "Adds a view that can be used to produce job listings."
  []
  (println "\t* adding the job_listings view.")
  (exec-raw
   "CREATE VIEW job_listings AS
    SELECT j.id,
           j.job_name,
           j.app_name,
           j.start_date,
           j.end_date,
           j.status,
           j.deleted,
           u.username,
           j.job_description,
           j.app_id,
           j.app_wiki_url,
           j.app_description,
           j.result_folder_path,
           j.submission,
           CASE WHEN COUNT(DISTINCT t.name) > 1 THEN 'DE'
                ELSE MAX(t.name)
           END AS job_type
    FROM jobs j
         JOIN users u ON j.user_id = u.id
         JOIN job_steps s ON j.id = s.job_id
         JOIN job_types t ON s.job_type_id = t.id
    GROUP BY j.id, u.username"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing conversion for" version)
  (add-job-listings-view))

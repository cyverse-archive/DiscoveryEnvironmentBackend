(ns clj-icat-direct.queries
  (:require [clojure.string :as string]))

(defn- get-folder-like-clause
  "Returns a LIKE clause for folder paths in the count-filtered-items-in-folder query."
  []
  "c.coll_name LIKE ?")

(defn- get-folder-path-clause
  "Returns an equals clause for folder paths in the count-filtered-items-in-folder query."
  []
  "c.coll_name = ?")

(defn get-filtered-paths-where-clause
  "Returns a LIKE clause for folder paths in the count-filtered-items-in-folder query."
  [filter-files filtered-paths]
  (string/join " OR " (concat (repeat (count filter-files)
                                      (get-folder-like-clause))
                              (repeat (count filtered-paths)
                                      (get-folder-path-clause)))))

(defn filter-files->query-args
  "Converts the list of filter-files for use as arguments in the count-filtered-items-in-folder
   query."
  [filter-files]
  (map (partial str "%/") filter-files))

(defn filter-chars->sql-char-class
  "Returns a regex character class set for use in the count-filtered-items-in-folder query
   using the given characters."
  [filter-chars]
  (str "[" (string/replace filter-chars
                           #"'|\[|\]|\\"
                           {"\\" "\\\\"
                            "'" "\\'"
                            "[" "\\["
                            "]" "\\]"})
       "]"))


(def queries
  {:count-items-in-folder
   "WITH user_groups AS ( SELECT g.*
                            FROM r_user_main u
                            JOIN r_user_group g ON g.user_id = u.user_id
                           WHERE u.user_name = ?
                             AND u.zone_name = ? ),

         parent      AS ( SELECT * from r_coll_main
                           WHERE coll_name = ? ),

         data_objs   AS ( SELECT *
                            FROM r_data_main
                           WHERE coll_id = ANY(ARRAY( SELECT coll_id FROM parent )))

    SELECT count(*) AS total
      FROM ( SELECT DISTINCT d.data_id FROM r_objt_access a
               JOIN data_objs d ON a.object_id = d.data_id
              WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
                AND a.object_id IN ( SELECT data_id from data_objs )
              UNION
             SELECT DISTINCT c.coll_id FROM r_coll_main c
               JOIN r_objt_access a ON c.coll_id = a.object_id
               JOIN parent p ON c.parent_coll_name = p.coll_name
              WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
                AND c.coll_type != 'linkPoint' ) AS contents"

   :count-all-items-under-folder
   "WITH user_groups AS ( SELECT g.*
                            FROM r_user_main u
                            JOIN r_user_group g ON g.user_id = u.user_id
                           WHERE u.user_name = ?
                             AND u.zone_name = ? ),

         parent      AS ( SELECT * from r_coll_main
                           WHERE coll_name = ?
                          UNION
                          SELECT * from r_coll_main
                           WHERE coll_name LIKE ? || '/%' ),

         data_objs   AS ( SELECT *
                            FROM r_data_main
                           WHERE coll_id = ANY(ARRAY( SELECT coll_id FROM parent )))

    SELECT count(*) AS total
      FROM ( SELECT DISTINCT d.data_id FROM r_objt_access a
               JOIN data_objs d ON a.object_id = d.data_id
              WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
                AND a.object_id IN ( SELECT data_id from data_objs )
              UNION
             SELECT DISTINCT c.coll_id FROM r_coll_main c
               JOIN r_objt_access a ON c.coll_id = a.object_id
               JOIN parent p ON c.parent_coll_name = p.coll_name
              WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
                AND c.coll_type != 'linkPoint' ) AS contents"

   :count-filtered-items-in-folder
   "WITH user_groups AS ( SELECT g.* FROM r_user_main u
                            JOIN r_user_group g ON g.user_id = u.user_id
                           WHERE u.user_name = ?
                             AND u.zone_name = ? ),

         parent      AS ( SELECT * from r_coll_main
                           WHERE coll_name = ? ),

         data_objs   AS ( SELECT *
                            FROM r_data_main
                           WHERE coll_id = ANY(ARRAY( SELECT coll_id FROM parent )))

    SELECT count(*) AS total_filtered
      FROM ( SELECT DISTINCT d.data_id FROM r_objt_access a
               JOIN data_objs d ON a.object_id = d.data_id
              WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
                AND a.object_id IN ( SELECT data_id FROM data_objs )
                AND d.data_name ~ ?
              UNION
             SELECT DISTINCT c.coll_id FROM r_coll_main c
               JOIN r_objt_access a ON c.coll_id = a.object_id
               JOIN parent p ON c.parent_coll_name = p.coll_name
              WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
                AND c.coll_type != 'linkPoint'
                AND (%s)) AS filtered"

   :list-folders-in-folder
   "WITH user_groups AS ( SELECT g.* FROM r_user_main u
                            JOIN r_user_group g ON g.user_id = u.user_id
                           WHERE u.user_name = ?
                             AND u.zone_name = ? ),

         parent      AS ( SELECT * from r_coll_main
                           WHERE coll_name = ? )

    SELECT DISTINCT
           c.parent_coll_name                     as dir_name,
           c.coll_name                            as full_path,
           regexp_replace(c.coll_name, '.*/', '') as base_name,
           c.create_ts                            as create_ts,
           c.modify_ts                            as modify_ts,
           'collection'                           as type,
           0                                      as data_size,
           a.access_type_id                       as access_type_id
      FROM r_coll_main c
      JOIN r_objt_access a ON c.coll_id = a.object_id
      JOIN parent p ON c.parent_coll_name = p.coll_name
     WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
       AND c.coll_type != 'linkPoint'"

   :count-files-in-folder
   "WITH user_groups AS ( SELECT g.*
                            FROM r_user_main u
                            JOIN r_user_group g ON g.user_id = u.user_id
                           WHERE u.user_name = ?
                             AND u.zone_name = ? ),

         parent      AS ( SELECT * from r_coll_main
                           WHERE coll_name = ? ),

         data_objs   AS ( SELECT *
                            FROM r_data_main
                           WHERE coll_id = ANY(ARRAY( SELECT coll_id FROM parent )))

      SELECT count(DISTINCT d.data_id) FROM r_objt_access a
        JOIN data_objs d ON a.object_id = d.data_id
       WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
         AND a.object_id IN ( SELECT data_id from data_objs )"

   :count-folders-in-folder
   "WITH user_groups AS ( SELECT g.*
                            FROM r_user_main u
                            JOIN r_user_group g ON g.user_id = u.user_id
                           WHERE u.user_name = ?
                             AND u.zone_name = ? ),

         parent      AS ( SELECT * from r_coll_main
                           WHERE coll_name = ? )

    SELECT count(DISTINCT c.coll_id) FROM r_coll_main c
      JOIN r_objt_access a ON c.coll_id = a.object_id
      JOIN parent p ON c.parent_coll_name = p.coll_name
     WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
       AND c.coll_type != 'linkPoint'"

   :file-permissions
   "SELECT DISTINCT o.access_type_id, u.user_name
      FROM r_user_main u,
           r_data_main d,
           r_coll_main c,
           r_tokn_main t,
           r_objt_access o
     WHERE c.coll_name = ?
       AND d.data_name = ?
       AND c.coll_id = d.coll_id
       AND o.object_id = d.data_id
       AND t.token_namespace = 'access_type'
       AND u.user_id = o.user_id
       AND o.access_type_id = t.token_id
     LIMIT ?
    OFFSET ?"

   :folder-permissions
   "SELECT DISTINCT a.access_type_id, u.user_name
     FROM r_coll_main c
     JOIN r_objt_access a ON c.coll_id = a.object_id
     JOIN r_user_main u ON a.user_id = u.user_id
    WHERE c.parent_coll_name = ?
      AND c.coll_name = ?
    LIMIT ?
   OFFSET ?"

   :folder-permissions-for-user
   "WITH user_lookup AS ( SELECT u.user_id as user_id FROM r_user_main u WHERE u.user_name = ?)
    SELECT DISTINCT a.access_type_id
      FROM r_coll_main c
      JOIN r_objt_access a ON c.coll_id = a.object_id
      JOIN r_user_main u ON a.user_id = u.user_id
     WHERE c.coll_name = ?
       AND u.user_id IN ( SELECT g.group_user_id
                           FROM  r_user_group g,
                                 user_lookup
                           WHERE g.user_id = user_lookup.user_id )"

   :file-permissions-for-user
   "WITH user_lookup AS ( SELECT u.user_id as user_id FROM r_user_main u WHERE u.user_name = ? ),
              parent AS ( SELECT c.coll_id as coll_id, c.coll_name as coll_name FROM r_coll_main c WHERE c.coll_name = ? )
    SELECT DISTINCT a.access_type_id
      FROM r_data_main d
      JOIN r_coll_main c ON c.coll_id = d.coll_id
      JOIN r_objt_access a ON d.data_id = a.object_id
      JOIN r_user_main u ON a.user_id = u.user_id,
           user_lookup,
           parent
     WHERE u.user_id IN ( SELECT g.group_user_id
                           FROM  r_user_group g,
                                 user_lookup
                           WHERE g.user_id = user_lookup.user_id )
       AND c.coll_id = parent.coll_id
       AND d.data_name = ?"

   :paged-folder-listing
   "WITH user_groups AS ( SELECT g.*
                            FROM r_user_main u
                            JOIN r_user_group g ON g.user_id = u.user_id
                           WHERE u.user_name = ?
                             AND u.zone_name = ? ),

         parent      AS ( SELECT * from r_coll_main
                           WHERE coll_name = ? ),

         data_objs   AS ( SELECT *
                            FROM r_data_main
                           WHERE coll_id = ANY(ARRAY( SELECT coll_id FROM parent )))

    SELECT DISTINCT p.full_path,
                    p.base_name,
                    p.data_size,
                    p.create_ts,
                    p.modify_ts,
                    p.type
    FROM ( SELECT c.coll_name                       as dir_name,
                  c.coll_name || '/' || d.data_name as full_path,
                  d.data_name                       as base_name,
                  (array_agg(d.create_ts))[1]       as create_ts,
                  (array_agg(d.modify_ts))[1]       as modify_ts,
                  'dataobject'                      as type,
                  (array_agg(d.data_size))[1]       as data_size,
                  (array_agg(a.access_type_id))[1]  as access_type_id
             FROM r_objt_access a
             JOIN data_objs d ON a.object_id = d.data_id
             JOIN r_coll_main c ON c.coll_id = d.coll_id
            WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
              AND a.object_id IN ( SELECT data_id FROM data_objs )
         GROUP BY c.coll_name, d.data_name
            UNION
           SELECT c.parent_coll_name                     as dir_name,
                  c.coll_name                            as full_path,
                  regexp_replace(c.coll_name, '.*/', '') as base_name,
                  c.create_ts                            as create_ts,
                  c.modify_ts                            as modify_ts,
                  'collection'                           as type,
                  0                                      as data_size,
                  a.access_type_id                       as access_type_id
             FROM r_coll_main c
             JOIN r_objt_access a ON c.coll_id = a.object_id
             JOIN parent p ON c.parent_coll_name = p.coll_name
            WHERE a.user_id IN ( SELECT group_user_id FROM user_groups )
              AND c.coll_type != 'linkPoint') AS p
    ORDER BY p.type ASC, %s %s
       LIMIT ?
      OFFSET ?"})

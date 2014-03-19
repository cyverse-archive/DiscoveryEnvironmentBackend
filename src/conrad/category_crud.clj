(ns conrad.category-crud
  (:use [conrad.database]
        [conrad.app-listings])
  (:require [clojure.java.jdbc :as jdbc]))

(def trash-category-id "Trash")

(def
  ^{:private true}
   validate-category-name
  (partial validate-field-length "template_group" "name"))

(declare load-category load-subcategories)

(defn load-category-by-id [id]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group WHERE id = ?" id]
    (let [category (first rs)]
      (if (nil? category)
        (throw (IllegalArgumentException.
                (str "category, " id ", does not exist"))))
      category)))

(defn load-category-by-hid [hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group WHERE hid = ?" hid]
    (let [category (first rs)]
      (if (nil? category)
        (throw (IllegalStateException.
                (str "category with internal id, " hid ", does not exist"))))
      category)))

(defn- count-templates [hid subcategories]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) FROM template_group_template tgt
      JOIN transformation_activity a ON tgt.template_id = a.hid
      WHERE NOT a.deleted
      AND template_group_id = ?" hid]
    (+ (:count (first rs)) (reduce + (map #(:template_count %) subcategories)))))

(defn load-category [hid & fs]
  (jdbc/with-query-results rs
    ["SELECT * FROM analysis_group_listing WHERE hid = ?" hid]
    (let [f (first fs)
          subcategories (load-subcategories hid f)
          category (assoc (first rs)
                     :template_count (count-templates hid subcategories)
                     :groups subcategories)]
      (if (nil? f) category (f category)))))

(defn- load-subcategories [hid f]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group_group WHERE parent_group_id = ?" hid]
    (doall (map #(load-category (:subgroup_id %) f) rs))))

(defn get-public-root-category-hids []
  (jdbc/with-query-results rs
    ["SELECT * FROM workspace WHERE is_public IS TRUE"]
    (doall (map #(:root_analysis_group_id %) rs))))

(defn count-apps-in-category [hid]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) AS count
      FROM template_group_template tgt
      JOIN transformation_activity a ON tgt.template_id = a.hid
      WHERE tgt.template_group_id = ?
      AND a.deleted IS FALSE" hid]
    (:count (first rs))))

(defn count-subcategories-in-category [hid]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) AS count
      FROM template_group_group
      WHERE parent_group_id = ?" hid]
    (:count (first rs))))

(defn find-parent-category-hids [hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group_group
      WHERE subgroup_id = ?" hid]
    (doall (map #(:parent_group_id %) rs))))

(defn- load-parent-categories [child-hid]
  (jdbc/with-query-results rs
    ["SELECT tg.* FROM template_group tg
      JOIN template_group_group tgg ON tg.hid = tgg.parent_group_id
      WHERE tgg.subgroup_id = ?" child-hid]
    (doall rs)))

(defn ensure-category-doesnt-contain-apps [id hid]
  (if (not= 0 (count-apps-in-category hid))
    (throw (IllegalStateException. (str "category, " id ", contains apps")))))

(defn ensure-category-doesnt-contain-subcategories [id hid]
  (if (not= 0 (count-subcategories-in-category hid))
    (throw (IllegalStateException.
            (str "category, " id ", contains subcategories")))))

(defn ensure-empty [id hid]
  (ensure-category-doesnt-contain-apps id hid)
  (ensure-category-doesnt-contain-subcategories id hid))

(defn- get-subcategory-ids [parent-hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group_group
      WHERE parent_group_id = ?
      ORDER BY hid" parent-hid]
    (doall (map #(:subgroup_id %) rs))))

(defn- update-subcategory-hid [parent-id subcategory-id hid]
  (jdbc/update-values
   :template_group_group
   ["parent_group_id = ? AND subgroup_id = ?" parent-id subcategory-id]
   {:hid hid}))

(defn- ensure-contiguous-subcategory-hids-for-parent [parent-hid]
  (doseq [x (map vector (get-subcategory-ids parent-hid) (iterate inc 0))]
    (update-subcategory-hid parent-hid (first x) (last x))))

(defn- ensure-contiguous-subcategory-hids [parent-hids]
  (doseq [x parent-hids] (ensure-contiguous-subcategory-hids-for-parent x)))

(defn- is-descendent-of [possible-descendent-hid possible-ancestor-hid]
  (jdbc/with-query-results rs
    ["SELECT * FROM template_group_group
      WHERE parent_group_id = ?" possible-ancestor-hid]
    (some #(or (= (:subgroup_id %) possible-descendent-hid)
              (is-descendent-of possible-descendent-hid (:subgroup_id %))) rs)))

(defn- ensure-parent-not-descendent-of-child [parent child]
  (if (is-descendent-of (:hid parent) (:hid child))
    (throw (IllegalStateException.
            (str (:id parent) " is a descendent of " (:id child))))))

(defn- remove-category-from-parents [hid]
  (let [parent-hids (find-parent-category-hids hid)]
    (jdbc/delete-rows :template_group_group ["subgroup_id = ?" hid])
    (ensure-contiguous-subcategory-hids parent-hids)))

(defn- get-next-grouping-hid [parent-category-hid]
  (jdbc/with-query-results rs
    ["SELECT COALESCE(MAX(hid) + 1, 0) AS next_hid
      FROM template_group_group
      WHERE parent_group_id = ?" parent-category-hid]
    (:next_hid (first rs))))

(defn- group-category [parent-category-hid child-category-hid]
  (let [grouping-hid (get-next-grouping-hid parent-category-hid)]
    (jdbc/insert-values
     :template_group_group
     [:parent_group_id :subgroup_id :hid]
     [parent-category-hid child-category-hid grouping-hid])))

(defn- ensure-category-doesnt-exist [parent-id parent-hid name]
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) AS count FROM template_group_group tgg
      JOIN template_group tg ON tgg.subgroup_id = tg.hid
      WHERE tgg.parent_group_id = ?
      AND name = ?" parent-hid name]
    (if (> (:count (first rs)) 0)
      (throw (IllegalStateException.
              (str "category, " parent-id ", already contains a subcategory "
                   "named, \"" name "\""))))))

(defn update-category-name [hid name]
  (validate-category-name name)
  (dorun (map #(ensure-category-doesnt-exist (:id %) (:hid %) name)
              (load-parent-categories hid)))
  (jdbc/update-values
    :template_group ["hid = ?" hid]
    {:name name}))

(defn move-subcategory [parent-id child-id]
  (let [parent (load-category-by-id parent-id)
        child (load-category-by-id child-id)
        parent-hid (:hid parent)
        child-hid (:hid child)]
    (ensure-category-doesnt-exist parent-id parent-hid (:name child))
    (ensure-category-doesnt-contain-apps parent-id parent-hid)
    (ensure-parent-not-descendent-of-child parent child)
    (remove-category-from-parents child-hid)
    (group-category parent-hid child-hid)))

(defn delete-category-with-id [id]
  (let [category (load-category-by-id id)
        hid (:hid category)]
    (ensure-empty id hid)
    (remove-category-from-parents hid)
    (jdbc/delete-rows :template_group_template ["template_group_id = ?" hid])
    (jdbc/delete-rows :template_group ["hid = ?" hid])))

(defn- insert-category [args]
  (let [vals (conj (vec (map #(get args %) [:id :name :description])) 0)]
    (jdbc/insert-values
     :template_group
     [:id :name :description :workspace_id] vals)))

(defn- count-deleted-and-orphaned-apps []
  (jdbc/with-query-results rs
    ["SELECT COUNT(*) AS count FROM transformation_activity a
      WHERE  (a.deleted AND EXISTS (
          SELECT * FROM template_group_template tgt
          JOIN template_group tg ON tgt.template_group_id = tg.hid
          JOIN workspace w ON tg.workspace_id = w.id
          WHERE tgt.template_id = a.hid AND w.is_public))
      OR NOT EXISTS (
          SELECT * FROM template_group_template tgt
          WHERE a.hid = tgt.template_id)"]
    (:count (first rs))))

(defn validate-category-insertion [args]
  (let [parent-id (:parent-category-id args)
        parent-hid (:parent-category-hid args)
        name (:name args)]
    (validate-category-name name)
    (ensure-category-doesnt-exist parent-id parent-hid name)
    (ensure-category-doesnt-contain-apps parent-id parent-hid)))

(defn insert-and-group-category [args]
  (validate-category-insertion args)
  (insert-category args)
  (let [category (load-category-by-id (:id args))]
    (group-category (:parent-category-hid args) (:hid category))))

(defn get-app-ids-in-category [category-hid]
  (jdbc/with-query-results rs
    ["SELECT tgt.template_id
      FROM template_group_template tgt
      JOIN transformation_activity a ON tgt.template_id = a.hid
      WHERE NOT a.deleted
      AND template_group_id = ?" category-hid]
    (doall (map #(:template_id %) rs))))

(defn get-category-hid [id]
  (jdbc/with-query-results rs
    ["SELECT hid FROM template_group WHERE id = ?" id]
    (if (empty? rs)
      (throw (IllegalArgumentException. (str "app group " id " not found")))
      (:hid (first rs)))))

(defn list-trash-category []
  {:template_count (count-deleted-and-orphaned-apps)
   :name "Trash"
   :groups []
   :id trash-category-id
   :is_public true
   :description "Deleted and orphaned apps"})

(defn is-orphaned-app [hid]
  (jdbc/with-query-results rs
    ["SELECT (NOT EXISTS (
         SELECT * FROM template_group_template tgt
         WHERE template_id = ?)) AS orphaned" hid]
    (:orphaned (first rs))))

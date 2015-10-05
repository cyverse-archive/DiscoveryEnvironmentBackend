(ns facepalm.c210-2015081201
  (:use [korma.core]
        [facepalm.error-codes :only [conversion-validation-error]]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150812.01")

(defn convert
  []
  (println "Performing the conversion for" version)
  ;; collect offending app_categories IDs
  (let [bad-categories (map :id (select :app_categories (fields [:id]) (where (= :name nil))))
        bad-workspaces (map :id (select :workspace (fields [:id]) (where {:root_category_id [in bad-categories]})))]
    ;; delete app_category_group rows where offending categories are children
    (delete :app_category_group (where {:child_category_id [in bad-categories]}))
    ;; VERIFY: no suggested_groups, app_category_app, or app_category_group rows referencing offending categories
    (let [bad-suggestions (map :app_category_id (select :suggested_groups (fields [:app_category_id]) (where {:app_category_id [in bad-categories]})))
          bad-apps (map :app_category_id (select :app_category_app (fields [:app_category_id]) (where {:app_category_id [in bad-categories]})))
          bad-groups (map :parent_category_id (select :app_category_group (fields [:parent_category_id]) (where {:parent_category_id [in bad-categories]})))]
      (if (> 0 (+ (count bad-suggestions) (count bad-apps) (count bad-groups)))
        (conversion-validation-error version
                                     {:error-code  :extra-groups-apps-suggestions-null-names})))
    ;; update (to null) workspaces' root_category_ids of offending workspaces, collect workspace IDs
    (update :workspace (set-fields {:root_category_id nil}) (where {:id [in bad-workspaces]}))
    ;; delete offending app categories
    (delete :app_categories (where (= :name nil)))
    ;; VERIFY: no app categories reference problem workspaces
    (let [bad-extra-categories (map :id (select :app_categories (fields [:id]) (where {:workspace_id [in bad-workspaces]})))]
      (if (> 0 (count bad-extra-categories))
        (conversion-validation-error version
                                     {:error-code :extra-categories-reference-bad-workspaces})))
    ;; delete workspaces
    (delete :workspace (where {:id [in bad-workspaces]})))
  ;; VERIFY: no remaining problems
  (let [bad-categories (map :id (select :app_categories (fields [:id]) (where (= :name nil))))]
    (if (> 0 (count bad-categories))
      (conversion-validation-error version {:error-code :not-all-categories-cleaned})))
  ;; add not-null constraint
  (exec-raw "ALTER TABLE app_categories ALTER COLUMN name SET NOT NULL"))

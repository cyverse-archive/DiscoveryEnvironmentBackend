(ns metadactyl.persistence.app-metadata
  "Persistence layer for app metadata."
  (:use [kameleon.entities]
        [kameleon.uuids :only [uuidify]]
        [korma.core]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.assertions]
        [metadactyl.util.conversions :only [remove-nil-vals]])
  (:require [metadactyl.persistence.app-metadata.relabel :as relabel]))

(defn get-app
  "Retrieves an app from the database."
  [app-id]
  (assert-not-nil
   [:app-id app-id]
   (first
     (select app_listing
             (fields :id
                     :name
                     :description
                     :integrator_email
                     :step_count)
             (where {:id app-id})))))

(defn get-integration-data
  "Retrieves integrator info from the database, adding it first if not already there."
  [{:keys [email first-name last-name]}]
  (if-let [integration-data (first (select integration_data (where {:integrator_email email})))]
    integration-data
    (insert integration_data (values {:integrator_name (str first-name " " last-name)
                                      :integrator_email email}))))

(defn add-app
  "Adds top-level app info to the database and returns the new app info, including its new ID."
  [app]
  (let [integration-data-id (:id (get-integration-data current-user))
        app (-> app
                (select-keys [:name :description])
                (assoc :integration_data_id integration-data-id
                       :edited_date (sqlfn now)))]
    (insert apps (values app))))

(defn add-step
  "Adds an app step to the database for the given app ID."
  [app-id step-number step]
  (let [step (-> step
                 (select-keys [:task_id])
                 (assoc :app_id app-id
                        :step step-number))]
    (insert app_steps (values step))))

(defn add-mapping
  "Adds an input/output workflow mapping to the database for the given app source->target mapping."
  [mapping]
  (let [workflow-map (select-keys mapping [:app_id :source_step :target_step])
        mapping-id (:id (insert :workflow_io_maps (values workflow-map)))]
    (dorun
      (for [[input output] (:map mapping)]
        (insert :input_output_mapping (values {:mapping_id mapping-id
                                               :input (uuidify input)
                                               :output (uuidify output)}))))))

(defn update-app
  "Updates top-level app info in the database."
  [app]
  (let [app-id (:id app)
        app (-> app
                (select-keys [:name :description])
                (assoc :edited_date (sqlfn now))
                (remove-nil-vals))]
    (update apps (set-fields app) (where {:id app-id}))))

(defn remove-app-steps
  "Removes all steps from an App. This delete will cascade to workflow_io_maps and
   input_output_mapping entries."
  [app-id]
  (delete app_steps (where {:app_id app-id})))

(defn get-full-app
  "Retrieves all app listing fields from the database."
  [app-id]
  (assert-not-nil [:app-id app-id] (first (select app_listing (where {:id app-id})))))

(defn update-app-labels
  "Updates the labels in an app."
  [req]
  (relabel/update-app-labels req))

(defn app-accessible-by
  "Obtains the list of users who can access an app."
  [app-id]
  (map :username
       (select [:apps :a]
               (join [:app_category_app :aca]
                     {:a.id :aca.app_id})
               (join [:app_categories :g]
                     {:aca.app_category_id :g.id})
               (join [:workspace :w]
                     {:g.workspace_id :w.id})
               (join [:users :u]
                     {:w.user_id :u.id})
               (fields :u.username)
               (where {:a.id app-id}))))

(defn delete-app
  "Marks an app as deleted in the metadata database."
  [app-id]
  (update :apps
          (set-fields {:deleted true})
          (where {:id app-id})))

(defn rate-app
  "Adds or updates a user's rating and comment ID for the given app."
  [app-id user-id request]
  (let [rating (first (select ratings (where {:app_id app-id, :user_id user-id})))]
    (if rating
      (update ratings
              (set-fields (remove-nil-vals request))
              (where {:app_id app-id
                      :user_id user-id}))
      (insert ratings
              (values (assoc (remove-nil-vals request) :app_id app-id, :user_id user-id))))))

(defn delete-app-rating
  "Removes a user's rating and comment ID for the given app."
  [app-id user-id]
  (delete ratings
    (where {:app_id app-id
            :user_id user-id})))

(defn get-app-avg-rating
  "Gets the average and total number of user ratings for the given app ID."
  [app-id]
  (first
    (select ratings
            (fields (raw "CAST(COALESCE(AVG(rating), 0.0) AS DOUBLE PRECISION) AS average"))
            (aggregate (count :rating) :total)
            (where {:app_id app-id}))))

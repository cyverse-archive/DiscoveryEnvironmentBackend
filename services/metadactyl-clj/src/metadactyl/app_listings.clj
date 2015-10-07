(ns metadactyl.app-listings
  (:use [slingshot.slingshot :only [try+ throw+]]
        [korma.core :exclude [update]]
        [kameleon.core]
        [kameleon.entities]
        [kameleon.app-groups]
        [kameleon.app-listing]
        [kameleon.uuids :only [uuidify]]
        [metadactyl.persistence.app-documentation :only [get-documentation]]
        [metadactyl.persistence.app-metadata :only [get-app get-app-tools]]
        [metadactyl.tools :only [get-tools-by-id]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.config]
        [metadactyl.util.conversions :only [to-long remove-nil-vals]]
        [metadactyl.workspace]))

(def my-public-apps-id (uuidify "00000000-0000-0000-0000-000000000000"))
(def trash-category-id (uuidify "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"))

(defn format-trash-category
  "Formats the virtual group for the admin's deleted and orphaned apps category."
  [workspace-id params]
  {:id         trash-category-id
   :name       "Trash"
   :is_public  true
   :app_count  (count-deleted-and-orphaned-apps)})


(defn- load-app-details
  "Retrieves the details for a single app."
  [app-id]
  (assert-not-nil [:app-id app-id]
    (first (select apps
                   (with app_references)
                   (with integration_data)
                   (where {:id app-id})))))

(defn- format-wiki-url
  "CORE-6510: Remove the wiki_url from app details responses if the App has documentation saved."
  [{:keys [id wiki_url] :as app}]
  (assoc app :wiki_url (if (get-documentation id) nil wiki_url)))

(defn- with-task-params
  "Includes a list of related file parameters in the query's result set,
   with fields required by the client."
  [query task-param-entity]
  (with query task-param-entity
              (join :parameter_values {:parameter_values.parameter_id :id})
              (fields :id
                      :name
                      :label
                      :description
                      :required
                      :parameter_values.value
                      [:data_format :format])))

(defn- get-tasks
  "Fetches a list of tasks for the given IDs with their inputs and outputs."
  [task-ids]
  (select tasks
    (fields :id
            :name
            :description)
    (with-task-params inputs)
    (with-task-params outputs)
    (where (in :id task-ids))))

(defn- format-task-file-param
  [file-parameter]
  (dissoc file-parameter :value))

(defn- format-task-output
  [{value :value :as output}]
  (-> output
    (assoc :label value)
    format-task-file-param))

(defn- format-task
  [task]
  (-> task
    (update-in [:inputs] (partial map (comp remove-nil-vals format-task-file-param)))
    (update-in [:outputs] (partial map (comp remove-nil-vals format-task-output)))))

(defn get-tasks-with-file-params
  "Fetches a formatted list of tasks for the given IDs with their inputs and outputs."
  [task-ids]
  (map format-task (get-tasks task-ids)))

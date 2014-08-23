(ns mescal.agave-de-v2.app-listings
  (:require [mescal.agave-de-v2.constants :as c]
            [mescal.util :as util]))

(defn hpc-app-group
  []
  {:description  c/hpc-group-description
   :id           c/hpc-group-id
   :is_public    true
   :name         c/hpc-group-name
   :task_count   -1
   :workspace_id "00000000-0000-0000-0000-000000000000"})

(defn get-app-name
  [{:keys [name id]}]
  (str name " [" id "]"))

(defn- format-app-listing
  [statuses jobs-enabled? listing]
  (let [mod-time (util/parse-timestamp (:lastModified listing))
        system   (:executionSystem listing)]
    (-> listing
        (dissoc :lastModified :name :shortDescription :revision :executionSystem :isPublic :version
                :_links)
        (assoc
            :name                 (get-app-name listing)
            :app_type             "External"
            :can_run              true
            :can_favor            false
            :can_rate             false
            :deleted              false
            :description          (:shortDescription listing)
            :disabled             (not (and jobs-enabled? (= "UP" (statuses system))))
            :edited_date          mod-time
            :group_id             c/hpc-group-id
            :group_name           c/hpc-group-name
            :integration_date     mod-time
            :integrator_email     c/unknown-value
            :integrator_name      c/unknown-value
            :is_favorite          false
            :is_public            (:isPublic listing)
            :pipeline_eligibility {:is_valid true}
            :rating               {:average 0.0}
            :step-count           1
            :wiki_url             ""))))

(defn list-apps
  [agave statuses jobs-enabled?]
  (let [listing  (.listApps agave)
        total    (count listing)
        listing  (map (partial format-app-listing statuses jobs-enabled?) listing)]
    (assoc (hpc-app-group)
      :templates      listing
      :template_count total)))

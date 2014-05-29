(ns mescal.agave-de-v2
  (:require [mescal.util :as util]))

(def ^:private hpc-group-description "Apps that run on HPC resources.")
(def ^:private hpc-group-name "High-Performance Computing")
(def ^:private hpc-group-id "HPC")
(def ^:private unknown-value "UNKNOWN")

(def ^:private hpc-group-overview
  {:id   hpc-group-id
   :name hpc-group-name})

(defn hpc-app-group
  []
  {:description    hpc-group-description
   :id             hpc-group-id
   :is_public      true
   :name           hpc-group-name
   :template_count -1
   :workspace_id   0})

(defn- get-app-name
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
            :can_run              true
            :can_favor            false
            :can_rate             false
            :deleted              false
            :description          (:shortDescription listing)
            :disabled             (not (and jobs-enabled? (= "UP" (statuses system))))
            :edited_date          mod-time
            :group_id             hpc-group-id
            :group_name           hpc-group-name
            :integration_date     mod-time
            :integrator_email     unknown-value
            :integrator_name      unknown-value
            :is_favorite          false
            :is_public            (:isPublic listing)
            :pipeline_eligibility {:is_valid false :reason "HPC App"}
            :rating               {:average 0.0}
            :step-count           1
            :wiki_url             ""))))

(defn- get-system-statuses
  [agave]
  (into {} (map (juxt :id :status) (.listSystems agave))))

(defn list-apps
  [agave jobs-enabled?]
  (let [listing  (.listApps agave)
        total    (count listing)
        statuses (get-system-statuses agave)
        listing  (map (partial format-app-listing statuses jobs-enabled?) listing)]
    (assoc (hpc-app-group)
      :templates      listing
      :template_count total)))

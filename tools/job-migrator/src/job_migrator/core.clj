(ns job-migrator.core
  (:gen-class)
  (:use [korma.core]
        [slingshot.slingshot :only [throw+ try+]])
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [clojure-commons.error-codes :as ce]
            [job-migrator.config :as config]
            [job-migrator.db :as db]
            [job-migrator.oauth-persistence :as op]
            [job-migrator.osm :as osm]
            [mescal.de :as agave]))

(def de-job-type "DE")
(def agave-job-type "Agave")

(def ^:private cli-options
  [["-c" "--config PATH" "Configuration path."
    :default  "/etc/iplant/de/donkey.properties"
    :parse-fn str]])

(defn- get-config-path [args]
  (let [{:keys [options _ errors _]} (cli/parse-opts args cli-options)]
    (when errors
      (println (string/join "\n" errors))
      (System/exit 1))
    (:config options)))

(defn- add-new-job-fields [job]
  (update :jobs
          (set-fields {:app_id             (:app-id job)
                       :app_description    (:app-description job)
                       :app_wiki_url       (:app-wiki-url job)
                       :result_folder_path (:result-folder-path job)})
          (where {:id (:id job)})))

(defn- delete-job [job-id]
  (update :jobs
          (set-fields {:deleted true})
          (where {:id job-id})))

(defn- list-jobs []
  (select [:jobs :j]
          (join [:job_steps :s] {:j.id :s.job_id})
          (join [:job_types :t] {:s.job_type_id :t.id})
          (join [:users :u] {:j.user_id :u.id})
          (fields [:j.id          :id]
                  [:s.external_id :external-id]
                  [:u.username    :username]
                  [:t.name        :job-type])
          (where {:j.deleted false})))

(defn- get-app-wiki-url [app-id]
  (or (-> (select :analysis_listing
                  (fields :wikiurl)
                  (where {:id app-id}))
          (first)
          (:wikiurl))
      ""))

(defn- get-new-job-fields-from-osm [{:keys [external-id] :as job}]
  (let [job-state (osm/get-job external-id)
        app-id    (:analysis_id job-state)]
    (assoc job
      :app-id             app-id
      :app-description    (:analysis_description job-state)
      :app-wiki-url       (get-app-wiki-url app-id)
      :result-folder-path (:output_dir job-state))))

(defn- get-access-token
  [{:keys [api-name] :as server-info} username]
  (if-let [token-info (op/get-access-token api-name username)]
    (assoc (merge server-info token-info)
      :token-callback  (partial op/store-access-token api-name username))
    (throw+ (IllegalStateException. (str "user " username " has no access token")))))

(defn- get-agave-client [user]
  (agave/de-agave-client-v2
   (config/agave-base-url)
   (config/agave-storage-system)
   (partial get-access-token (config/agave-oauth-settings) user)
   (config/agave-jobs-enabled)))

(defn- get-new-job-fields-from-agave [{:keys [id external-id username] :as job}]
  (try+
   (let [agave     (get-agave-client username)
         job-state (.listJob agave external-id)]
     (assoc job
       :app-id             (:analysis_id job-state)
       :app-description    (:analysis_details job-state)
       :app-wiki-url       (:wiki_url job-state)
       :result-folder-path (:resultfolderid job-state)))
   (catch Object e
     (println e)
     (println "WARNING: deleting job" id)
     (delete-job id))))

(defn- get-new-job-fields [{:keys [job-type] :as job}]
  (condp = job-type
    de-job-type    (get-new-job-fields-from-osm job)
    agave-job-type (get-new-job-fields-from-agave job)))

(defn -main [& args]
  (config/load-config-from-file (get-config-path args))
  (db/define-database)
  (dorun (map (comp add-new-job-fields get-new-job-fields) (take 5 (list-jobs)))))

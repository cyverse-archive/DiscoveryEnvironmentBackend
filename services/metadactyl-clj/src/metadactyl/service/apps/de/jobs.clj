(ns metadactyl.service.apps.de.jobs
  (:use [clojure.string :only [split-lines]]
        [clojure-commons.file-utils :only [build-result-folder-path path-join]]
        [kameleon.jobs :only [get-job-type-id save-job save-job-step]]
        [kameleon.queries :only [get-user-id]]
        [medley.core :only [dissoc-in]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [korma.core]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [kameleon.db :as db]
            [kameleon.uuids :as uuids]
            [me.raynes.fs :as fs]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.service.apps.de.jobs.base :as jb]
            [metadactyl.util.config :as config]
            [metadactyl.util.json :as json-util]
            [metadactyl.util.service :as service]))

(defn- secured-params
  [user]
  {:user (:shortUsername user)})

(defn- pre-process-jex-step
  "Removes the input array of a fAPI step's config."
  [{{step-type :type} :component :as step}]
  (if (= step-type "fAPI")
    (dissoc-in step [:config :input])
    step))

(defn- pre-process-jex-submission
  "Finalizes the job for submission to the JEX."
  [job]
  (update-in job [:steps] (partial map pre-process-jex-step)))

(defn- do-jex-submission
  [job]
  (try+
    (http/post (config/jex-base-url)
               {:body         (cheshire/encode (pre-process-jex-submission job))
                :content-type :json})
    (catch Object o
      (log/error (:throwable &throw-context) "job submission failed")
      (throw+ {:error_code ce/ERR_REQUEST_FAILED}))))

(defn- store-submitted-job
  "Saves information about a job in the database."
  [user job submission status]
  (let [job-info (-> job
                     (select-keys [:parent_id :app_id :app_name :app_description :notify])
                     (assoc :job_name           (:name job)
                            :job_description    (:description job)
                            :app_wiki_url       (:wiki_url job)
                            :result_folder_path (:output_dir job)
                            :start_date         (sqlfn now)
                            :user_id            (get-user-id (:username user))
                            :status             status))]
    (save-job job-info (cheshire/encode submission))))

(defn- store-job-step
  "Saves a single job step in the database."
  [job-id job status]
  (save-job-step {:job_id          job-id
                  :step_number     1
                  :external_id     (:uuid job)
                  :start_date      (sqlfn now)
                  :status          status
                  :job_type_id     (get-job-type-id "DE")
                  :app_step_number 1}))

(defn- save-job-submission
  "Saves a DE job and its job-step in the database."
  ([user job submission]
   (save-job-submission user job submission "Submitted"))
  ([user job submission status]
   (transaction
     (let [job-id (:id (store-submitted-job user job submission status))]
       (store-job-step job-id job status)
       job-id))))

(defn- get-batch-output-dir
  "Builds the parent output folder path for batch jobs, and creating it if it doesn't exist."
  [user submission]
  (let [output-dir (build-result-folder-path submission)]
    (try+
      (http/post (service/build-url (config/data-info-base-url) "stat-gatherer")
        {:query-params (secured-params user)
         :body (cheshire/encode {:paths [output-dir]})
         :content-type :json
         :as :stream})
      (catch Object does-not-exist
        (http/post (service/build-url (config/data-info-base-url) "data" "directory" "create")
          {:query-params (secured-params user)
           :body (cheshire/encode {:path output-dir})
           :content-type :json
           :as :stream})))
    output-dir))

(defn- get-path-list-contents
  "Gets the contents of the given path list as a vector of each of its paths (assuming one per line)"
  [user path]
  (when-not (empty? path)
    (try+
      (->> (http/get (service/build-url (config/data-info-base-url) "entries" "path" path)
             {:query-params (secured-params user)
              :as :stream})
           (:body)
           (slurp)
           (split-lines)
           (remove empty?)
           (drop 1))
      (catch Object o
        (log/error (:throwable &throw-context) "job submission failed")
        (throw+ {:error_code ce/ERR_REQUEST_FAILED
                 :message "Could get file contents of path list input"})))))

(defn- get-file-stats
  [user paths]
  (when-not (empty? paths)
    (try+
      (-> (http/post (service/build-url (config/data-info-base-url) "stat-gatherer")
            {:query-params (secured-params user)
             :body (cheshire/encode {:paths paths})
             :content-type :json
             :as :stream})
          (:body)
          (service/parse-json))
      (catch Object o
        (log/error (:throwable &throw-context) "job submission failed")
        (throw+ {:error_code ce/ERR_REQUEST_FAILED
                 :message "Could not lookup info types of inputs"})))))

(defn- validate-path-list-stats
  [file-stats]
  (when (> (:file-size file-stats) (config/path-list-max-size))
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :message    (str "HT Analysis Path List file exceeds maximum size of "
                              (config/path-list-max-size)
                              " bytes.")
             :path       (:path file-stats)
             :file-size  (:file-size file-stats)})))

(defn- validate-path-list-input-multiplicity
  "Throws an error if any of the given inputs have a multiplicity of 'many' and one of the path-list
  paths as its value."
  [path-list-paths inputs]
  (when (some #(contains? path-list-paths (:value %))
              (filter #(= "many" (:multiplicity %)) inputs))
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :message (str "HT Analysis Path List files are not supported in multi-file inputs.")})))

(defn- path-list-stats->validated-paths
  "Validates the given list of path-list stats, then extracts and validates their paths against the
   given list of job inputs, returning the validated paths."
  [path-list-stats inputs]
  (dorun (map validate-path-list-stats path-list-stats))
  (let [paths (set (map :path path-list-stats))]
    (validate-path-list-input-multiplicity paths inputs)
    paths))

(defn- get-path-list-stats
  [user job]
  (let [inputs (mapcat (comp :input :config) (:steps job))
        file-stats-map (get-file-stats user (set (map :value inputs)))
        file-stats (map second (:paths file-stats-map))]
    (filter #(= (:infoType %) (config/path-list-info-type)) file-stats)))

(defn- get-path-list-contents-map
  [user paths]
  (let [path-lists (into {} (map #(vector % (get-path-list-contents user %)) paths))
        first-list-count (count (second (first path-lists)))]
    (when (> first-list-count (config/path-list-max-paths))
      (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
               :message    (str "The HT Analysis Path List exceeds the maximum of "
                                (config/path-list-max-paths)
                                " allowed paths.")
               :path       (ffirst path-lists)
               :path-count first-list-count}))
    (when-not (every? #(= first-list-count %) (map (comp count second) path-lists))
      (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
               :message "All HT Analysis Path Lists must have the same number of paths."}))
    path-lists))

(defn- split-inputs-by-path-list
  [inputs path-lists]
  ((juxt filter remove) #(contains? path-lists (:value %)) inputs))

(defn- split-params-by-path-list
  [params path-lists]
  (let [path-list-base-names (set (map fs/base-name (keys path-lists)))]
    ((juxt filter remove) #(contains? path-list-base-names (:value %)) params)))

(defn- get-partitioned-submission-config-entries
  [path-lists config]
  ((juxt filter remove) (fn [[k v]] (contains? path-lists v)) config))

(defn- build-batch-partitioned-job-step
  [path-lists step]
  (let [config (:config step)
        inputs (:input config)
        params (:params config)
        [list-inputs inputs] (split-inputs-by-path-list inputs path-lists)
        [list-params params] (split-params-by-path-list params path-lists)]
    (assoc step :config (assoc config
                          :input  [list-inputs inputs]
                          :params [list-params params]))))

(defn- submit-one-de-job
  [user submission job]
  (try+
    (do-jex-submission job)
    (save-job-submission user job submission)
    (catch Object o
      (if (nil? (:parent_id job))
        (throw+ o)
        (save-job-submission user job submission "Failed")))))

(defn- update-batch-config
  [batch-path-map [list-params params]]
  (let [list-params (map (fn [[k v]] (vector k (get batch-path-map v))) list-params)]
    (into {} (concat list-params params))))

(defn- update-batch-input
  [batch-path-map input]
  (let [path (get batch-path-map (:value input))
        filename (when-not (nil? path) (fs/base-name path))]
    (assoc input
      :name     filename
      :property filename
      :value    path)))

(defn- update-batch-param
  [batch-filename-map param]
  (let [path (get batch-filename-map (:value param))
        filename (when-not (nil? path) (fs/base-name path))]
    (assoc param :value filename)))

(defn- update-batch-step
  [batch-path-map partitioned-step]
  (let [config (:config partitioned-step)
        [list-inputs inputs] (:input config)
        [list-params params] (:params config)
        batch-filename-map (into {} (map (fn [[k v]] (vector (fs/base-name k) v)) batch-path-map))
        list-inputs (map (partial update-batch-input batch-path-map) list-inputs)
        list-params (map (partial update-batch-param batch-filename-map) list-params)]
    (assoc partitioned-step :config (assoc config
                                      :input  (concat list-inputs inputs)
                                      :params (concat list-params params)))))

(defn- submit-job-in-batch
  [user submission job batch-job-id job-number & batch-paths]
  (let [batch-path-map (into {} batch-paths)
        job-suffix (str "analysis-" (inc job-number))
        job (assoc job :parent_id batch-job-id
                       :name (str (:name job) " - " job-suffix)
                       :output_dir (path-join (:output_dir job) job-suffix)
                       :steps (map (partial update-batch-step batch-path-map) (:steps job))
                       :uuid (uuids/uuid))
        submission (assoc submission
                     :config (update-batch-config batch-path-map (:config submission)))]
    (submit-one-de-job user submission job)))

(defn- path-list-map-entry->path-contents-pairs
  [[path-list-path path-list-contents]]
  (map (juxt (constantly path-list-path) identity) path-list-contents))

(defn- submit-batch-de-job
  [user {config :config :as submission} {steps :steps :as job} path-list-stats]
  (let [path-list-paths (path-list-stats->validated-paths path-list-stats
                                                          (mapcat (comp :input :config) steps))
        path-lists (get-path-list-contents-map user path-list-paths)
        transposed-list-path (map path-list-map-entry->path-contents-pairs path-lists)
        job (assoc job :output_dir (get-batch-output-dir user submission)
                       :create_output_subdir false
                       :group (config/jex-batch-group-name))
        batch-job-id (save-job-submission user job submission)
        job (assoc job :steps (map (partial build-batch-partitioned-job-step path-lists) steps))
        submission (assoc submission
                     :config (get-partitioned-submission-config-entries path-lists config))
        submit-batch-job (partial submit-job-in-batch user submission job batch-job-id)]
    (apply (partial map submit-batch-job (range)) transposed-list-path)))

(defn- submit-de-only-job
  [user submission job path-list-stats]
  (if (empty? path-list-stats)
    (submit-one-de-job user submission job)
    (submit-batch-de-job user submission job path-list-stats)))

(defn- format-job-submission-response
  [user jex-submission batch?]
  (remove-nil-vals
   {:app_description (:app_description jex-submission)
    :app_disabled    false
    :app_id          (:app_id jex-submission)
    :app_name        (:app_name jex-submission)
    :batch           batch?
    :description     (:description jex-submission)
    :id              (str (:uuid jex-submission))
    :name            (:name jex-submission)
    :notify          (:notify jex-submission)
    :resultfolderid  (:output_dir jex-submission)
    :startdate       (str (.getTime (db/now)))
    :status          jp/submitted-status
    :username        (:username user)
    :wiki_url        (:wiki_url jex-submission)}))

(defn- submit-job
  [user submission job]
  (let [de-only-job? (zero? (ap/count-external-steps (:app_id job)))
        path-list-stats (get-path-list-stats user job)]
    (if de-only-job?
      (submit-de-only-job user submission job path-list-stats)
      (if (empty? path-list-stats)
        (do-jex-submission job)
        (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
                 :message "HT Analysis Path Lists are not supported in Apps with Agave steps."})))
    (format-job-submission-response user job (empty? path-list-stats))))

(defn- prep-submission
  [submission]
  (assoc submission
    :output_dir           (build-result-folder-path submission)
    :create_output_subdir false))

(defn- build-submission
  [user submission]
  (remove-nil-vals (jb/build-submission user submission)))

(defn submit
  [user submission]
  (->> (prep-submission submission)
       (build-submission user)
       (json-util/log-json "job")
       (submit-job user submission)))

(defn submit-step
  [user submission]
  (let [job-step (build-submission user submission)]
    (json-util/log-json "job step" job-step)
    (do-jex-submission job-step)
    (:uuid job-step)))

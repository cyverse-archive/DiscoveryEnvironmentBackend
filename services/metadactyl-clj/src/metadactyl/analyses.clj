(ns metadactyl.analyses
  (:use [clojure.string :only [split-lines]]
        [kameleon.jobs :only [get-job-type-id save-job save-job-step]]
        [kameleon.queries :only [get-user-id]]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [korma.core]
        [korma.db :only [transaction]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [me.raynes.fs :as fs]
            [metadactyl.analyses.base :as ab]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.util.config :as config]
            [metadactyl.util.service :as service]))

(defn- secured-params
  []
  {:user (:shortUsername current-user)})

(defn- do-jex-submission
  [job]
  (try+
    (http/post (config/jex-base-url)
               {:body         (cheshire/encode job)
                :content-type :json})
    (catch Object o
      (log/error (:throwable &throw-context) "job submission failed")
      (throw+ {:error_code ce/ERR_REQUEST_FAILED}))))

(defn- store-submitted-job
  "Saves information about a job in the database."
  [job submission status]
  (let [job-info (-> job
                     (select-keys [:parent_id :app_id :app_name :app_description])
                     (assoc :job_name           (:name job)
                            :job_description    (:description job)
                            :app_wiki_url       (:wiki_url job)
                            :result_folder_path (:output_dir job)
                            :start_date         (sqlfn now)
                            :user_id            (get-user-id (:username current-user))
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
  ([job submission]
   (save-job-submission job submission "Submitted"))
  ([job submission status]
   (transaction
     (let [job-id (:id (store-submitted-job job submission status))]
       (store-job-step job-id job status)
       job-id))))

(defn- get-path-list-contents
  "Gets the contents of the given path list as a vector of each of its paths (assuming one per line)"
  [path]
  (when-not (empty? path)
    (try+
      (->> (http/get (log/spy (service/build-url (config/data-info-base-url) "entries" "path" path))
             {:query-params (secured-params)
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
  [paths]
  (when-not (empty? paths)
    (try+
      (-> (http/post (service/build-url (config/data-info-base-url) "stat-gatherer")
            {:query-params (secured-params)
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
    (throw+ {:error_code ce/ERR_REQUEST_FAILED
             :message    (str "HT Analysis Path List file exceeds maximum size of "
                              (config/path-list-max-size)
                              " bytes.")
             :path       (:path file-stats)
             :file-size  (:file-size file-stats)})))

(defn- find-path-lists
  [job]
  (let [inputs (log/spy (mapcat (comp :input :config) (:steps job)))
        file-stats (log/spy (get-file-stats (set (map :value inputs))))
        info-type-filter #(= (:infoType (second %)) (config/path-list-info-type))]
    (into {} (filter info-type-filter (:paths file-stats)))))

(defn- get-path-list-contents-map
  [paths]
  (let [path-lists (log/spy (into {} (map #(vector % (get-path-list-contents %)) paths)))
        first-list-count (count (second (first path-lists)))]
    (when (> first-list-count (config/path-list-max-paths))
      (throw+ {:error_code ce/ERR_REQUEST_FAILED
               :message    (str "The HT Analysis Path List exceeds the maximum of "
                                (config/path-list-max-paths)
                                " allowed paths.")
               :path       (ffirst path-lists)
               :path-count first-list-count}))
    (when-not (every? #(= first-list-count %) (map (comp count second) path-lists))
      (throw+ {:error_code ce/ERR_REQUEST_FAILED
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
        inputs (log/spy (:input config))
        params (log/spy (:params config))
        [list-inputs inputs] (log/spy (split-inputs-by-path-list inputs path-lists))
        [list-params params] (log/spy (split-params-by-path-list params path-lists))]
    (assoc step :config (assoc config
                          :input  [list-inputs inputs]
                          :params [list-params params]))))

(defn- submit-one-de-job
  [submission job]
  (try+
    (do-jex-submission job)
    (save-job-submission (log/spy job) submission)
    (catch Object o
      (if (nil? (:parent_id job))
        (throw+ o)
        (save-job-submission job submission "Failed")))))

(defn- update-batch-config
  [batch-path-map [list-params params]]
  (let [list-params (map (fn [[k v]] (vector k (get batch-path-map v))) list-params)]
    (into {} (concat list-params params))))

(defn- update-batch-input
  [batch-path-map input]
  (let [path (log/spy (get batch-path-map (:value input)))
        filename (when-not (nil? path) (fs/base-name path))]
    (assoc input
      :name     filename
      :property filename
      :value    path)))

(defn- update-batch-param
  [batch-filename-map param]
  (let [path (log/spy (get batch-filename-map (:value param)))
        filename (when-not (nil? path) (fs/base-name path))]
    (assoc param :value filename)))

(defn- update-batch-step
  [batch-path-map partitioned-step]
  (let [config (:config partitioned-step)
        [list-inputs inputs] (:input config)
        [list-params params] (:params config)
        batch-filename-map (log/spy (into {} (map (fn [[k v]] (vector (fs/base-name k) v)) batch-path-map)))
        list-inputs (map (partial update-batch-input batch-path-map) list-inputs)
        list-params (map (partial update-batch-param batch-filename-map) list-params)]
    (assoc partitioned-step :config (assoc config
                                      :input  (concat list-inputs inputs)
                                      :params (concat list-params params)))))

(defn- submit-job-in-batch
  [submission job batch-job-id & batch-paths]
  (let [batch-path-map (log/spy (into {} batch-paths))
        job (assoc job :parent_id batch-job-id
                       :steps (map (partial update-batch-step batch-path-map) (:steps job)))
        submission (assoc submission
                     :config (update-batch-config batch-path-map (:config submission)))]
    (submit-one-de-job submission job)))

(defn- path-list-map-entry->path-contents-pairs
  [[path-list-path path-list-contents]]
  (map (juxt (constantly path-list-path) identity) path-list-contents))

(defn- submit-batch-de-job
  [submission job path-lists]
  (dorun (map (comp validate-path-list-stats second) path-lists))
  (let [path-lists (get-path-list-contents-map (map (comp :path second) path-lists))
        transposed-list-path (map path-list-map-entry->path-contents-pairs path-lists)
        batch-job-id (save-job-submission job submission)
        job (assoc job :steps (map (partial build-batch-partitioned-job-step path-lists) (:steps job)))
        submission (assoc submission
                     :config (get-partitioned-submission-config-entries path-lists
                                                                        (:config submission)))]
    (dorun (apply (partial map (partial submit-job-in-batch submission job batch-job-id))
                  transposed-list-path))))

(defn- submit-de-only-job
  [submission job path-lists]
  (if (empty? path-lists)
    (submit-one-de-job submission job)
    (submit-batch-de-job submission job path-lists)))

(defn- submit-job
  [submission job]
  (let [de-only-job? (zero? (ap/count-external-steps (:app_id job)))
        path-lists (log/spy (find-path-lists job))]
    (if de-only-job?
      (submit-de-only-job submission job path-lists)
      (if (empty? path-lists)
        (do-jex-submission job)
        (throw+ {:error_code ce/ERR_REQUEST_FAILED
                 :message "HT Analysis Path Lists are not supported in Apps with Agave steps."}))))
  job)

(defn submit
  [{:keys [user email]} submission]
  (->> (ab/build-submission user email submission)
       (remove-nil-vals)
       (submit-job submission)))

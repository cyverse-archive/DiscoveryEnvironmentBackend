(ns jex.process
  (:use [jex.json-validator]
        [slingshot.slingshot :only [throw+]])
  (:require [jex.incoming-xforms :as ix]
            [jex.outgoing-xforms :as ox]
            [jex.dagify :as dagify]
            [jex.config :as cfg]
            [clojure.tools.logging :as log]
            [clojure.java.shell :as sh]
            [clj-http.client :as http]
            [cemerick.url :refer (url url-encode)]
            [cheshire.core :as cheshire]
            [clojure.string :as string]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]))

(defn failure [reason]
  {:status "failure" :reason reason})

(defn success []
  {:status "success"})

(def validators
  [#(string? (:uuid %))
   #(string? (:username %))
   #(string? (:name %))
   #(sequential? (:steps %))
   #(every? true?
            (for [step (:steps %)]
              (every? true?
                      [(map? (:config step))
                       (map? (:component step))
                       (string? (:location (:component step)))
                       (string? (:name (:component step)))])))])

(defn validate-submission
  "Validates a submission."
  [submit-map]
  (valid? submit-map validators))

(defn condor-env []
  {"PATH"          (cfg/path-env)
   "CONDOR_CONFIG" (cfg/condor-config)})

(defn push-job-to-jex-events
  [condor-id submitter app-id inv-id]
  (let [job-record {:condorid     condor-id
                    :submitter    submitter
                    :appid        app-id
                    :invocationid inv-id}
        post-url   (str (url (cfg/jex-events-url) "jobs"))
        result     (http/post post-url {:form-params job-record
                                        :content-type :json}
                              {:throw-exceptions false})]
    (log/info result)))

(defn condor-rm
  "Stops a condor job."
  [sub-id]
  (sh/with-sh-env (condor-env) (sh/sh "condor_rm" sub-id)))

(defn missing-condor-id
  [uuid]
  (hash-map
   :error_code "ERR_MISSING_CONDOR_ID"
   :uuid uuid))

(defn get-job-sub-id
  [uuid]
  (let [get-url (str (url (cfg/jex-events-url) "invocations" uuid))
        result  (cheshire/decode (:body (http/get get-url)) true)]
    (when-not result
      (throw+ (missing-condor-id uuid)))
    (when-not (contains? result :CondorID)
      (throw+ (missing-condor-id uuid)))
    (:CondorID result)))

(defn stop-analysis
  "Calls condor_rm on the submission id associated with the provided analysis
   id."
  [uuid]
  (log/warn "Received request to stop job" uuid)
  (let [sub-id (get-job-sub-id uuid)]
    (log/debug (str "Grabbed Condor ID: " sub-id))
    (if sub-id
      (let [{:keys [exit out err]} (condor-rm sub-id)]
        (when-not (= exit 0)
          (log/debug (str "condor-rm status was: " exit))
          (throw+ {:error_code "ERR_FAILED_NON_ZERO"
                   :sub_id sub-id
                   :out out
                   :err err})))
      (throw+ {:error_code "ERR_MISSING_CONDOR_ID" :uuid uuid}))))

(defn param?
  "Returns true of the object passed in is actually a param."
  [param-map]
  (and
   (contains? param-map :name)
   (contains? param-map :value)
   (contains? param-map :order)))

(defn cmdline-preview
  "Accepts a map in the following format:
   {:params [
       {:name \"-t\"
        :value \"foo\"
        :order 0
   ]}

   Returns a map in the format:
   {:params \"-t foo\"}"
  [param-obj]
  (when-not (contains? param-obj :params)
    (throw+ {:error_code "ERR_INVALID_JSON"
             :message "Missing params key."}))

  (when-not (every? true? (map param? (:params param-obj)))
    (throw+ {:error_code "ERR_INVALID_JSON"
             :message "Objects must have 'name', 'value', and 'order' keys."}))

  (hash-map :params (ix/escape-params (ix/param-maps (:params param-obj)))))

(defn handle-submit-results
  "Logs the result of the call to condor_submit."
  [{:keys [exit out err]}]
  (log/warn "Exit code of condor-submit:" exit)
  (log/warn "condor_submit stdout:\n" out)
  (log/warn "condor-submit-dag stderr:\n" err)
  (if (pos? exit)
    (throw (Exception. "condor_submit exited with a non-zero status."))))

(defn parse-sub-id
  "Parses out the submission id from the output of the condor_submit."
  [cout]
  (last (re-find #"\d+ job\(s\) submitted to cluster (\d+)\." cout)))

(defn submission-id
  "Grabs the submission id from the output using (parse-sub-id), logs it, and
   returns it."
  [{:keys [out]}]
  (let [sub-id (parse-sub-id out)]
    (log/warn (str "Grabbed dag_id: " sub-id))
    sub-id))

(defn generate-submission
  "Takes in the analysis map, transforms it for script generation, creates the
   iplant.sh and iplant.cmd. Returns the modified analysis map."
  [submit-map]
  (let [result (-> submit-map ix/transform dagify/dagify)]
    (log/info "Output map:")
    (log/info (cheshire/encode (last result)))
    result))

(defn condor-submit
  "Submits a job to Condor. sub-path should be the path to a Condor submission
   file."
  [work-dir sub-path]
  (let [result (sh/with-sh-env (condor-env) (sh/sh "/usr/local/bin/submitnerator.sh" work-dir sub-path))]
    (handle-submit-results result)
    result))

(defn submit
  "Applies the incoming tranformations to the submitted request, submits the
   job to the Condor cluster, applies outgoing transformations, and dumps the
   resulting map to the OSM."
  [submit-map]
  (let [[sub-path updated-map] (generate-submission submit-map)
        sub-result (condor-submit (str (fs/parent sub-path)) sub-path)
        sub-id     (submission-id sub-result)]
    (log/warn "Submitted Job:" sub-id)
    (log/warn "Pushing to jex-events:\n\tCondorID:" sub-id
              "\n\tUsername:" (:username updated-map)
              "\n\tAppID:"  (:app_id updated-map)
              "\n\tInvocationID:" (:uuid updated-map))
    (try
      (push-job-to-jex-events sub-id (:username updated-map) (:app_id updated-map) (:uuid updated-map))
      (catch Exception e
        (log/warn "Exception caught when sending event to jex-events, cancelling job:" e)
        (condor-rm sub-id)
        (throw e)))
    [(:exit sub-result) sub-id]))

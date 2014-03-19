(ns jex.process
  (:use [jex.json-validator]
        [slingshot.slingshot :only [throw+]])
  (:require [jex.incoming-xforms :as ix]
            [jex.outgoing-xforms :as ox]
            [jex.dagify :as dagify]
            [jex.config :as cfg]
            [clojure.tools.logging :as log]
            [clojure.java.shell :as sh]
            [clojure-commons.osm :as osm]
            [cheshire.core :as cheshire]))

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

(defn create-osm-record
  "Creates a new record in the OSM, associates the notification-url with it as a
   callback, and returns the OSM document ID in a string."
  [osm-client]
  (let [notif-url (cfg/notif-url)
        doc-id    (osm/save-object osm-client {})
        result    (osm/add-callback osm-client doc-id "on_update" notif-url)]
    (log/warn result)
    doc-id))

(defn condor-rm
  "Stops a condor job."
  [sub-id]
  (sh/with-sh-env (condor-env) (sh/sh "condor_rm" sub-id)))

(defn extract-state-from-result
  "Extracts the state from the JSON returned by the OSM."
  [result-map]
  (-> result-map (cheshire/decode true) :objects first :state))

(defn query-for-analysis
  "Queries the OSM for the document representing a particular analysis."
  [uuid]
  (osm/query
   (osm/create (cfg/osm-url) (cfg/osm-coll))
   {:state.uuid uuid}))

(defn missing-condor-id
  [uuid]
  (hash-map
   :error_code "ERR_MISSING_CONDOR_ID"
   :uuid uuid))

(defn get-job-sub-id
  [uuid]
  (let [osm-client (osm/create (cfg/osm-url) (cfg/osm-coll))
        result     (cheshire/decode
                    (osm/query osm-client {:state.uuid uuid})
                    true)]
    (when-not result
      (throw+ (missing-condor-id uuid)))

    (when-not (contains? result :objects)
      (throw+ (missing-condor-id uuid)))

    (when-not (pos? (count (:objects result)))
      (throw+ (missing-condor-id uuid)))

    (when-not (contains? (first (:objects result)) :state)
      (throw+ (missing-condor-id uuid)))

    (when-not (contains? (:state (first (:objects result))) :sub_id)
      (throw+ (missing-condor-id uuid)))

    (get-in (first (:objects result)) [:state :sub_id])))


(defn stop-analysis
  "Calls condor_rm on the submission id associated with the provided analysis
   id."
  [uuid]
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

(defn log-submit-results
  "Logs the result of the call to condor_submit."
  [{:keys [exit out err]}]
  (log/warn (str "Exit code of condor-submit: " exit))
  (log/info (str "condor-submit-dag stdout:"))
  (log/info out)
  (log/info (str "condor-submit-dag stderr:"))
  (log/info err))

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

(defn xform-map-for-osm
  "Transforms the analysis map so it's usable by panopticon."
  [updated-map sub-id]
  (ox/transform (assoc updated-map :sub_id sub-id)))

(defn push-failed-submit-to-osm
  "Pushes out the analysis map to OSM after marking it as Failed."
  [output-map]
  (let [osm-client (osm/create (cfg/osm-url) (cfg/osm-coll))
        doc-id     (create-osm-record osm-client)]
    (osm/update-object osm-client doc-id (assoc output-map :status "Failed"))
    doc-id))

(defn push-successful-submit-to-osm
  "Pushes out the analysis map to the OSM. It's marked as Submitted at this
   point."
  [output-map]
  (let [osm-client (osm/create (cfg/osm-url) (cfg/osm-coll))
        doc-id     (create-osm-record osm-client)]
    (osm/update-object osm-client doc-id output-map)
    doc-id))

(defn push-submission-info-to-osm
  "Decides whether to push the analysis map to the OSM based on the exit
   code of the condor_submit call."
  [output-map {:keys [exit]}]
  (if (not= exit 0)
    (push-failed-submit-to-osm output-map)
    (push-successful-submit-to-osm output-map)))

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
  [sub-path]
  (let [result (sh/with-sh-env (condor-env) (sh/sh "condor_submit" sub-path))]
    (log-submit-results result)
    result))

(defn submit
  "Applies the incoming tranformations to the submitted request, submits the
   job to the Condor cluster, applies outgoing transformations, and dumps the
   resulting map to the OSM."
  [submit-map]
  (let [[sub-path updated-map] (generate-submission submit-map)
        sub-result (condor-submit sub-path)
        sub-id     (submission-id sub-result)
        doc-id     (push-submission-info-to-osm
                    (xform-map-for-osm updated-map sub-id)
                    sub-result)]
    (log/warn "Submitted Job:" sub-id "OSM doc:" doc-id)
    [(:exit sub-result) sub-id doc-id]))

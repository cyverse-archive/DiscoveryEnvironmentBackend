(ns panopticon.core
  (:gen-class)
  (:use [clojure-commons.error-codes])
  (:require [cheshire.core :as cheshire]
            [clojure-commons.osm :as osm]
            [clojure-commons.props :as props]
            [clojure-commons.clavin-client :as cl]
            [clojure-commons.file-utils :as ft]
            [clojure.tools.logging :as log]
            [clojure.java.shell :as sh]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-time.core :as ct]
            [clj-time.format :as ctf]))

(def props (atom nil))

(def SUBMITTED "Submitted")
(def UNEXPANDED "Unexpanded")
(def IDLE "Idle")
(def RUNNING "Running")
(def REMOVED "Removed")
(def COMPLETED "Completed")
(def HELD "Held")
(def SUBERR "Submission_err")
(def FAILED "Failed")

(def JOBSTATUS
  {"0" UNEXPANDED
   "1" IDLE
   "2" RUNNING
   "3" REMOVED
   "4" COMPLETED
   "5" HELD
   "6" SUBERR})

(defn osm-url
  []
  (get @props "panopticon.osm.url"))

(defn osm-coll
  []
  (get @props "panopticon.osm.collection"))

(defn condor-config
  []
  (get @props "panopticon.condor.condor-config"))

(defn condor-q
  []
  (get @props "panopticon.condor.condor-q"))

(defn condor-history
  []
  (get @props "panopticon.condor.condor-history"))

(defn num-instances
  []
  (Integer/parseInt (get @props "panopticon.app.num-instances")))

(defn part-size
  []
  (if (get @props "panopticon.app.partition-size")
    (Integer/parseInt (get @props "panopticon.app.partition-size"))
    10))

;Converts a string to a boolean.
(def boolize #(boolean (Boolean. %)))

;Kinda like (every?)
(defn any? [pred coll] ((comp not not-any?) pred coll))

(defn osm-client [] (osm/create (osm-url) (osm-coll)))
(defn job-status [classad-map] (get JOBSTATUS (get classad-map "JobStatus")))

(defn running-jobs
  "Queries the OSM for a list of jobs that are in the
   Running, Submitted, or Idle states."
  []
  (try
    (let [query {"$or" [{"state.status" RUNNING}
                        {"state.status" SUBMITTED}
                        {"state.status" IDLE}]}]
      (:objects (cheshire/decode (osm/query (osm-client) query) true)))
    (catch java.lang.Exception e
      (log/warn e)
      [])))

(defn post-osm-updates
  "POSTs updated objects to the OSM. Input is a sequence of maps
   in the form:

   {:object_persistence_uuid string
    :state {}}

   In other words, you need to have the UUID associated with the
   doc in the OSM and the new state of the object."
  [osm-objects]
  (doseq [osm-object osm-objects]
    (try
      (let [new-state (:state osm-object)
            osm-id    (:object_persistence_uuid osm-object)]
        (if (nil? osm-id)
          (log/info (str "OSM state" new-state)))
        (osm/update-object (osm-client) osm-id new-state))
      (catch java.lang.Exception e
        (log/warn (format-exception e))))))

(defn classad-lines
  "Takes a string containing all of the classad information for a job
   and returns a list of strings. It splits the input string on \n and
   filters out any lines that begin with --."
  [classad-str]
  (filterv
    #(not (re-find #"^--" %))
    (string/split classad-str #"\n")))

(defn classad-maps
  "Transforms the output of either (queue) or (history) into
   a sequence of maps. Basically, the classads are parse so
   everything to the left of the first = is the key and
   everything to the right is the value."
  [job-output]
  (vec
    (for [classad-str (string/split job-output #"\n\n")]
      (apply
        merge
        (for [classad-line (classad-lines classad-str)]
          (let [sections (string/split classad-line #"\=")
                cl-key   (string/trim (first sections))
                cl-val   (-> (string/join "=" (rest sections))
                           (string/trim)
                           (string/replace #"^\"" "")
                           (string/replace #"\"$" ""))]
            {cl-key cl-val}))))))

(defn constraint
  [uuid]
  (str "IpcUuid ==\"" uuid "\""))

(defn all-constraints
  [uuids]
  (str (string/join " || " (mapv constraint uuids))))

(defn full-partition
  "Partitions collection into a vectory of seqs part-size in length.
   If the partition isn't clean, then the last seq will be smaller
   than the rest. This prevents jobs from being missed. Note:
   (clojure.core/partition) is lazy, but this function is not."
  [coll part-size]
  (vec (partition part-size part-size [] coll)))

(defn- run-history
  "Runs condor_history looking for single uuid, parses the output,
   and returns a sequence of maps created by (classad-maps)."
  [uuids]
  (let [args    ["condor_history" "-l" "-constraint" (all-constraints uuids)]
        results (apply sh/sh args)]
    (log/warn "[panopticon]" args)
    (log/info (str "Exit Code: " (:exit results)))
    (log/info (str "stderr: " (:err results)))
    (log/info (str "stdout: " (:out results)))
    (classad-maps (string/trim (:out results)))))

(defn history
  "Iterates over the list of uuids and calls condor_history for each one.
   Uses map to call condor_history. Returns a list of maps created by
   (classad-maps)."
  [uuids]
  (log/warn "[panopticon]" (count uuids))
  (vec (flatten (map run-history (full-partition uuids (part-size)))))
  #_(vec (flatten (map run-history uuids))))

(defn- run-queue
  "Runs condor_q looking for a single uuid, parses the output,
   and returns a sequence of maps created by (classad-maps)."
  [uuids]
  (let [args    ["condor_q" "-long" "-constraint" (all-constraints uuids)]
        results (apply sh/sh args)]
    (log/warn "[panopticon]" args)
    (log/info (str "Exit Code: " (:exit results)))
    (log/info (str "stderr: " (:err results)))
    (log/info (str "stdout: " (:out results)))
    (classad-maps (string/trim (:out results)))))

(defn queue
  "Iterates over the list of uuids and calls condor_q for each one.
   Uses pmap to call condor_q. Returns a list of maps created by
   (classad-maps)."
  [uuids]
  (log/warn "[panopticon]" (count uuids))
  (vec (flatten (map run-queue (full-partition uuids (part-size)))))
  #_(vec (flatten (map run-queue uuids))))

(defn condor-rm
  "Calls condor_rm on a dag."
  [sub-id]
  (let [cmd     ["condor_rm" sub-id]
        results (apply sh/sh cmd)]
    (log/warn "[panopticon]" cmd)
    (log/warn "[panopticon]" (str "Exit Code: " (:exit results)))
    (log/warn "[panopticon]" (str "stderr: " (:err results)))
    (log/warn "[panopticon]" (str "stdout: " (:out results)))))

(defn file-metadata-arg
  [meta-seq]
  (let [args (atom [])]
    (doseq [m meta-seq]
      (reset! args (concat @args ["-m" (string/join "," [(:attr m) (:value m) (:unit m)])])))
    @args))

(defn panop-exec
  ([args]
    (panop-exec args false))
  ([args stringify?]
    (if-not stringify?
      (apply sh/sh (flatten args))
      (string/join " " (flatten args)))))

(def put-env 
  {"PATH" (str "/usr/local/bin:/usr/local2/bin:/usr/local3/bin:" (System/getenv "PATH"))})


(defn transfer
  "Calls porklock to transfer a directory of files into iRODS."
  [source-dir output-dir user skip-parent-meta? meta-maps config-path]
  (when (ft/exists? source-dir)
    (let [exect      "/usr/local/bin/porklock"
          skip-pmeta (if skip-parent-meta? " --skip-parent-meta " "")
          meta-args  (file-metadata-arg meta-maps)
          all-args   [exect 
                      "put"
                      "--user" user 
                      "--source" source-dir 
                      "--destination" output-dir
                      skip-pmeta
                      meta-args
                      "--config" config-path 
                      :dir source-dir
                      :env put-env]
          results    (panop-exec all-args)]
      (log/warn "[panopticon] [metadata] " meta-maps)
      (log/warn "[panopticon] [metadata] " meta-args)
      (log/warn "[panopticon] [metadata]" (panop-exec all-args true))
      (log/warn "[panopticon]" "Exit Code: " (:exit results))
      (log/warn "[panopticon]" "stderr: " (:err results))
      (log/warn "[panopticon]" "stdout: " (:out results)))))

(defn rm-dir
  "Uses Apache Commons IO to recursively delete a directory. Does not play nice
   with NFS mountpoints."
  [dir-path]
  (let [dobj (clojure.java.io/file dir-path)]
    (try
      (org.apache.commons.io.FileUtils/deleteDirectory dobj)
      (catch java.lang.Exception e
        (log/warn e)))))

(defn de-job-status
  "Takes in single classad-map generated from (classad-maps)
   and translates the Condor status into a DE job state."
  [classad]
  (let [condor-status  (job-status classad)
        exit-by-signal (boolize (get classad "ExitBySignal"))
        exit-code      (get classad "ExitCode")]
    (cond
      (and (= condor-status COMPLETED) exit-by-signal)       FAILED
      (and (= condor-status COMPLETED) (not= exit-code "0")) FAILED
      (= condor-status HELD)    FAILED
      (= condor-status SUBERR)  FAILED
      (= condor-status REMOVED) FAILED
      :else                     condor-status)))

(defn- status-matches? [[job-key job] status] (= (:status job) status))
(defn- seq-jobs [obj] (seq (:jobs (:state obj))))

(defn no-jobs-queued?
  "Calls (queue) and determines if there are no more jobs remaining
   in the queue for a DAG."
  [uuid]
  (let [queued (queue [uuid])]
    (and
      (= (count queued) 1)
      (nil? (first queued)))))

(defn classads-for-osm-object
  "Takes in an osm-object and a list of classad maps and filters
   the classad maps down to just those that are pertinent to the
   analysis represented by the osm-object."
  [osm-object all-classads]
  (let [osm-uuid (:uuid (:state osm-object))]
    (filterv #(= (get % "IpcUuid") osm-uuid) all-classads)))

(def date-formatter
  (ctf/formatter "EEE MMM dd YYYY HH:mm:ss 'GMT'Z (z)" (ct/default-time-zone)))

(defn add-completion-date
  "Takes in an osm-obj and adds the completion date if the analysis
   the osm-obj represents is in the COMPLETED or FAILED states."
  [osm-obj]
  (let [curr-status (get-in osm-obj [:state :status])]
    (if (or (= curr-status COMPLETED) (= curr-status FAILED) (= curr-status HELD))
      (assoc-in osm-obj [:state :completion_date] (ctf/unparse date-formatter (ct/now)))
      osm-obj)))

(defn add-held-status
  "Takes in an osm-object and a classad and sets the [:state :held] field to true
   or false depending on whether the job is HELD."
  [osm-obj classad]
  (let [condor-status (job-status classad)]
    (if (= condor-status HELD)
      (assoc-in osm-obj [:state :held] true)
      (assoc-in osm-obj [:state :held] false))))

(defn update-osm-obj
  "Takes in an osm-object and a list of classad maps and returns
   a new version of the :jobs sub-map with all of the jobs updated
   with info from the classads. Called by (update-jobs) below."
  [osm-obj classads]
  (apply merge
    (vec
      (for [classad classads]
        (-> osm-obj
          (assoc-in [:state :status] (de-job-status classad))
          (assoc-in [:state :exit-code] (get classad "ExitCode"))
          (assoc-in [:state :exit-by-signal] (get classad "ExitBySignal"))
          add-completion-date
          (add-held-status classad))))))

(defn update-osm-objects
  "Takes in a list of osm-objects and a list of classad maps. Returns
   a list of osm-objects that have been updated with information from
   the classads that apply to them. Incidentally, this is where the
   (analysis-status) function is called to determine the overall state
   of an analysis."
  [osm-objects all-classads]
  (vec
    (filter
      #(not (nil? %))
      (for [osm-obj osm-objects]
        (let [classads (classads-for-osm-object osm-obj all-classads)]
          (if (pos? (count classads))
            (update-osm-obj osm-obj classads)))))))

(defn cleanup
  "Takes in a list of osm-objects and performs clean up actions based on the
   status of the analysis they represent."
  [osm-objects]
  (doseq [osm-object osm-objects]
    (clojure.pprint/pprint osm-object)
    (let [jstatus (get-in osm-object [:state :status])
          held?   (get-in osm-object [:state :held])
          sub-id  (get-in osm-object [:state :sub_id])
          ldir    (get-in osm-object [:state :condor-log-dir])
          wdir    (get-in osm-object [:state :working_dir])
          odir    (get-in osm-object [:state :output_dir])
          monitor (get-in osm-object [:state :monitor_transfer_logs])
          skip?   (get-in osm-object [:state :skip-parent-meta])
          meta    (get-in osm-object [:state :file-metadata])
          user    (get-in osm-object [:state :user])
          cfg     (ft/path-join wdir "logs" "irods-config")
          xfer?   (if (nil? monitor) true monitor)]
      (log/warn "[panopticon] [metadata] " meta)
      (cond
        held?
        (do
          (log/warn "[panopticon]" (str "Analysis " sub-id " is in the HELD state."))
          (when sub-id
            (condor-rm sub-id))
          (when (and wdir odir)
            (transfer wdir odir user skip? meta cfg))
          (when (and ldir odir)
            (transfer ldir odir user skip? meta cfg))
          (comment (rm-dir ldir)))

        (= jstatus FAILED)
        (do
          (when (and wdir odir)
            (transfer wdir odir user skip? meta cfg))
          (when (and ldir odir)
            (transfer ldir odir user skip? meta cfg))
          (comment (rm-dir ldir)))

        (= jstatus COMPLETED)
        (do
          (when (and wdir odir xfer?)
            (transfer wdir odir user skip? meta cfg))
          (when (and ldir odir xfer?)
            (transfer ldir odir user skip? meta cfg))
          (comment (rm-dir ldir))))))
  osm-objects)

(defn filter-classads
  "Simple function that filters a list of classad maps so
   it only contains classad maps for jobs that were run through
   the DE."
  [classads]
  (vec (filter #(contains? % "IpcUuid") classads)))

(defn sleep-duration
  []
  (Long/parseLong (or (get @props "panopticon.app.sleep-duration")
                      "5000")))

(defn -main
  [& args]
  (def zkprops (props/parse-properties "zkhosts.properties"))
  (def zkurl (get zkprops "zookeeper"))
  (def sleep-duration-memo (memoize sleep-duration))

  (log/info "Starting up. Reading configuration from Zookeeper.")

  (cl/with-zk
    zkurl
    (when-not (cl/can-run?)
      (log/warn "THIS APPLICATION CANNOT RUN ON THIS MACHINE. SO SAYETH ZOOKEEPER.")
      (log/warn "THIS APPLICATION WILL NOT EXECUTE CORRECTLY.")
      (System/exit 1))

    (reset! props (cl/properties "panopticon")))

  (log/info "Done reading configuration from Zookeeper.")
  (log/info (str "OSM Client: " (osm-client)))

  (log/warn "[panopticon]" "Checking for porklock...")
  (when-not (ft/exists? "/usr/local/bin/porklock")
    (log/warn "[panopticon]" "Could not find /usr/local/bin/porklock. Exiting")
    (System/exit 1))
  (log/warn "[panopticon]" "porklock found.")

  (loop []
    (try
      (let [osm-objects (running-jobs)]
        (when (pos? (count osm-objects))
          (let [osm-uuids (mapv #(:uuid (:state %)) osm-objects)
                classads  (filter-classads (concat (queue osm-uuids) (history osm-uuids)))]
            (-> osm-objects
              (update-osm-objects classads)
              (cleanup)
              (post-osm-updates)))))
      (catch java.lang.Exception e
        (log/warn (format-exception e))))
    (log/warn "[panopticon]" "Beginning sleep...")
    (Thread/sleep (sleep-duration-memo))
    (log/warn "[panopticon]" "Done sleeping.")
    (recur)))

(ns jex.dagify
  (:use [clojure-commons.file-utils :as ut]
        [clojure.string :only (join split trim blank?)]
        [jex.common])
  (:require [jex.config :as cfg]
            [clojure.tools.logging :as log]
            [me.raynes.fs :as fs])
  (:import [java.io File]))

(defn irods-config
  []
  (str "porklock.irods-host = " (cfg/irods-host) "\n"
       "porklock.irods-port = " (cfg/irods-port) "\n"
       "porklock.irods-user = " (cfg/irods-user) "\n"
       "porklock.irods-pass = " (cfg/irods-pass) "\n"
       "porklock.irods-home = " (cfg/irods-base) "\n"
       "porklock.irods-zone = " (cfg/irods-zone) "\n"
       "porklock.irods-resc = " (cfg/irods-resc) "\n"))

(defn irods-config-path
  "Generates the path to the irods-config file."
  [{condor-log-dir :condor-log-dir}]
  (ut/path-join condor-log-dir "logs" "irods-config"))

(defn script-output
  "Returns the path to the log containing the Condor logging
   from out on the execution nodes."
  [script-dir]
  "logs/script-output.log")

(defn script-error
  "Returns the path to the error log containing the Condor error
   logging from out on the execution nodes."
  [script-dir]
  "logs/script-error.log")

(defn script-log
  "Returns the path to the log containing the Condor logging
   from the submission node (contains the return value of the script)."
  [local-log-dir]
  (str (fs/file local-log-dir "script-condor-log")))

(defn ipc-exe
  [{:keys [steps] :as amap}]
  (println (first steps))
  (str "+IpcExe = \"" (get-in (first steps) [:component :name]) "\"\n"))

(defn ipc-exe-path
  [{:keys [steps] :as amap}]
  (str "+IpcExePath = \"" (get-in (first steps) [:component :location]) "\"\n"))

(defn script-submission
  "Generates the Condor submission file that will execute the generated
   shell script."
  [{:keys [username uuid local-log-dir] :as analysis-map}]
  (str
   "universe = vanilla\n"
   "executable = /bin/bash\n"
   "rank = mips\n"
   "arguments = \"iplant.sh\"\n"
   "output = script-output.log\n"
   "error = script-error.log\n"
   "log = condor.log\n"
   "request_disk = " (cfg/request-disk) "\n"
   "+IpcUuid = \"" uuid "\"\n"
   "+IpcJobId = \"generated_script\"\n"
   "+IpcUsername = \"" username "\"\n"
   (if (and (contains? analysis-map :group)
            (not (blank? (:group analysis-map))))
     (str "+AccountingGroup = \"" (:group analysis-map) "." username "\"\n"))
   "concurrency_limits = " username "\n"
   (ipc-exe analysis-map)
   (ipc-exe-path analysis-map)
   "should_transfer_files = YES\n"
   "transfer_input_files = iplant.sh,irods-config,iplant.cmd\n"
   "transfer_output_files = logs/de-transfer-trigger.log,logs/output-last-stdout,logs/output-last-stderr\n"
   "when_to_transfer_output = ON_EXIT_OR_EVICT\n"
   "notification = NEVER\n"
   "queue\n"))

(defn jobs-in-order
  "Take in the submitted analysis (processed by jex.incoming-xforms),
   and returns a list of the job definitions in the order that they
   should be executed in the shell script."
  [analysis-map]
  (concat (:all-input-jobs analysis-map)
          (:steps analysis-map)
          [(:final-output-job analysis-map)]))

(defn script-line
  "Takes in a job definition and generates a section of the shell
   script that will be executed out on the Condor nodes. This also
   handles capturing the exit value of a command in the shell script."
  [job-def]
  (let [exec   (:executable job-def)
        args   (:arguments job-def)
        stderr (:stderr job-def)
        stdout (:stdout job-def)]
    (str exec " " args " 1> " stdout " 2> " stderr "\n"
         "if [ ! \"$?\" -eq \"0\" ]; then\n"
             "\tEXITSTATUS=1\n"
         "fi\n")))

(def fail-script
  (str "if [ ! \"$?\" -eq \"0\" ]; then\n"
           "\tEXITSTATUS=1\n"
           "\texit $EXITSTATUS\n"
       "fi\n"))

(def rearrange-working-dir
  (str "if [ -e \"iplant.sh\" ]; then\n"
           "\tmv iplant.sh logs/\n"
       "fi\n"
       "if [ -e \"iplant.cmd\" ]; then\n"
           "\tmv iplant.cmd logs/\n"
       "fi\n"))

(defn docker-pull
  [image-name]
  (str "docker pull " image-name "\n" fail-script))

(defn- dc-pull
  [name tag]
  (str "docker pull " name ":" tag "\n"
       fail-script))

(defn data-containers-pull-sh
  [{data-containers :data-containers :as condor-map}]
  (if (pos? (count data-containers))
    (join "" (map #(dc-pull (:name %1) (:tag %1)) data-containers))
    ""))

(defn- dc-vol-host?
  [dc-map]
  (and (contains? dc-map :host_path)
       (not (blank? (:host_path dc-map)))))

(defn dc-vol-container?
  [dc-map]
  (and (contains? dc-map :container_path)
       (not (blank? (:container_path dc-map)))))

(defn- dc-vol?
  [dc-map]
  (or (dc-vol-host? dc-map)
      (dc-vol-container? dc-map)))

(defn dc-vol-read-only?
  [dc-map]
  (if (contains? dc-map :read_only)
    (:read_only dc-map)
    false))

(defn- dc-create
  [uuid dc-map]
  (str
   "docker create "

   (if (dc-vol? dc-map)
     "-v ")

   (cond
     (and (dc-vol-host? dc-map)
          (dc-vol-container? dc-map)
          (dc-vol-read-only? dc-map))
     (str (:host_path dc-map) ":" (:container_path dc-map) ":ro ")

     (and (dc-vol-host? dc-map)
          (dc-vol-container? dc-map)
          (not (dc-vol-read-only? dc-map)))
     (str (:host_path dc-map) ":" (:container_path dc-map) " ")

     (and (dc-vol-container? dc-map)
          (not (dc-vol-host? dc-map))
          (dc-vol-read-only? dc-map))
     (str (:container_path dc-map) ":ro ")

     (and (dc-vol-container? dc-map)
          (not (dc-vol-host? dc-map))
          (not (dc-vol-read-only? dc-map)))
     (str (:container_path dc-map) " ")

     :else "")

   "--name " (volumes-from-name uuid (:name_prefix dc-map)) " "
   (:name dc-map) ":" (:tag dc-map) "\n"
   fail-script))

(defn data-containers-create
  [{uuid :uuid data-containers :data-containers :as condor-map}]
  (if (pos? (count data-containers))
    (join "" (map (partial dc-create uuid) data-containers))
    ""))

(defn- dc-rm
  [uuid dc-map]
  (str "docker rm " (volumes-from-name uuid (:name_prefix dc-map))))

(defn data-containers-rm
  [{uuid :uuid data-containers :data-containers :as condor-map}]
  (if (pos? (count data-containers))
    (join "\n" (map (partial dc-rm uuid) data-containers))
    ""))

(defn script
  "Takes in an analysis map that has been processed by
   (jex.incoming-xforms/transform) and turns it into a shell script
   that will be run out on the Condor cluster. Needs refactoring."
  [analysis-map]
  (let [job-uuid  (:uuid analysis-map)
        job-dir   (str "iplant-de-jobs/" (:username analysis-map) "/" job-uuid)
        irods-cfg (irods-config-path analysis-map)
        data-container-name (str "data-" job-uuid)]
    (str
     "#!/bin/bash\n"
     "set -x\n"
     "readonly IPLANT_USER=" (:username analysis-map) "\n"
     "export IPLANT_USER\n"
     "readonly IPLANT_EXECUTION_ID=" (:uuid analysis-map) "\n"
     "export IPLANT_EXECUTION_ID\n"
     "export SCRIPT_LOCATION=${BASH_SOURCE}\n"
     "EXITSTATUS=0\n"
     "if [ -e /data2 ]; then ls /data2; fi\n"
     "mkdir -p logs\n"
     fail-script
     "ls -al > logs/de-transfer-trigger.log\n"
     fail-script
     rearrange-working-dir
     (data-containers-pull-sh analysis-map) "\n"
     (join "" (map docker-pull (seq (:container-images analysis-map))))
     (data-containers-create analysis-map)
     (join "\n" (map script-line (jobs-in-order analysis-map)))
     (data-containers-rm analysis-map) "\n"
     "hostname\n"
     "ps aux\n"
     "echo -----\n"
     "for i in $(ls logs); do\n"
     "    echo logs/$i\n"
     "    cat logs/$i\n"
     "    echo -----\n"
          "done\n"
     "exit $EXITSTATUS\n")))

(defn local-log-dir
  "Create the path to the local directory containing the condor logs."
  [{condor-log :condor-log-dir}]
  (str (fs/file condor-log "logs")))

(defn create-submission-directory
  "Creates the local directory where the iplant.sh and iplant.cmd files get
   written out to."
  [analysis-map]
  (let [dag-log-dir (local-log-dir analysis-map)]
    (log/info "Creating submission directories:" dag-log-dir)
    (fs/mkdirs dag-log-dir)
    analysis-map))

(defn scriptpath
  "Generates the path to the iplant.sh file."
  [analysis-map]
  (str (fs/file (local-log-dir analysis-map) "iplant.sh")))

(defn script-command-file
  "Generates the path to the iplant.cmd file."
  [analysis-map]
  (str (fs/file (local-log-dir analysis-map) "iplant.cmd")))

(defn generate-script-submission
  "Generates and writes out the iplant.sh and iplant.cmd files."
  [analysis-map]
  (spit (scriptpath analysis-map) (script analysis-map))
  (log/warn "Wrote bash script to" (scriptpath analysis-map))
  (spit (irods-config-path analysis-map) (irods-config))
  (log/warn "Wrote iRODS config to" (irods-config-path analysis-map))
  (spit
   (script-command-file analysis-map)
   (script-submission
    (merge
     analysis-map
     {:script-path (scriptpath analysis-map)
      :local-log-dir (local-log-dir analysis-map)})))
  (log/warn "Write HTCondor submission file to" (script-command-file analysis-map))
  analysis-map)

(defn cleanup-analysis-map
  "Cleans up the analysis map."
  [analysis-map]
  (-> analysis-map
      (dissoc :steps)
      (assoc :executable "/bin/bash"
             :args "iplant.sh"
             :status "Submitted"
             :output "script-output.log"
             :error "script-error.log"
             :log "condor.log")))

(defn dagify
  "Takes in analysis map that's been processed by
   (jex.incoming-xforms/transform)
   and puts together the stuff needed to submit the job to the Condor cluster.
   That includes:

   * Creating a place on the NFS mount point where the script and the Condor
     logs will be written to out on the Condor cluster.

   * Creating the local log directory (where Condor logs job stuff to on the
     submission node).

   * Generates the shell script and writes it out to the NFS mount point.

   * Generates the Condor submission file and writes it out to the NFS mount
     point.

   * Removes entries from the analysis-map that aren't needed any more.

   Returns a vector containing two entries, the path to the Condor submission
   file and the new version of the analysis-map."
  [analysis-map]
  (-> analysis-map
      (create-submission-directory)
      (generate-script-submission))
  [(script-command-file analysis-map)
   (cleanup-analysis-map analysis-map)])

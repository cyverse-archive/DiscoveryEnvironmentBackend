(ns jex.dagify
  (:use [clojure-commons.file-utils :as ut]
        [clojure.string :only (join)]
        [clojure.tools.logging :as log])
  (:require [jex.config :as cfg])
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
  [{working-dir :working_dir}]
  (ut/path-join working-dir "logs" "irods-config"))

(defn script-output
  "Returns the path to the log containing the Condor logging
   from out on the execution nodes."
  [script-dir] 
  (ut/path-join script-dir "logs" "script-output.log"))

(defn script-error 
  "Returns the path to the error log containing the Condor error
   logging from out on the execution nodes."
  [script-dir] 
  (ut/path-join script-dir "logs" "script-error.log"))

(defn script-log
  "Returns the path to the log containing the Condor logging
   from the submission node (contains the return value of the script)."
  [local-log-dir]
  (ut/path-join local-log-dir "script-condor-log"))

(defn ipc-exe
  [{:keys [steps] :as amap}]
  (str "+IpcExe = \"" (ut/basename (:executable (first steps))) "\"\n"))

(defn ipc-exe-path
  [{:keys [steps] :as amap}]
  (str "+IpcExePath = \"" (ut/dirname (:executable (first steps))) "\"\n"))

(defn script-submission
  "Generates the Condor submission file that will execute the generated
   shell script."
  [{:keys [username uuid working_dir script-path local-log-dir]
    :as analysis-map}]
  (str
   "universe = vanilla\n"
   "executable = /bin/bash\n" 
   "arguments = \"" script-path "\"\n"
   "output = " (script-output working_dir) "\n"
   "error = " (script-error working_dir) "\n"
   "log = " (script-log local-log-dir) "\n"
   "+IpcUuid = \"" uuid "\"\n"
   "+IpcJobId = \"generated_script\"\n"
   "+IpcUsername = \"" username "\"\n"
   (ipc-exe analysis-map)
   (ipc-exe-path analysis-map)
   "should_transfer_files = NO\n"
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
  (let [env    (:environment job-def)
        exec   (:executable job-def)
        args   (:arguments job-def)
        stderr (:stderr job-def)
        stdout (:stdout job-def)]
    (str env " " exec " " args " 1> " stdout " 2> " stderr "\n"
         "if [ ! \"$?\" -eq \"0\" ]; then\n"
             "\tEXITSTATUS=1\n"
         "fi\n")))

(def fail-script
  (str "if [ ! \"$?\" -eq \"0\" ]; then\n"
           "\tEXITSTATUS=1\n"
           "\texit $EXITSTATUS\n"
       "fi\n"))

(defn script
  "Takes in an analysis map that has been processed by
   (jex.incoming-xforms/transform) and turns it into a shell script
   that will be run out on the Condor cluster. Needs refactoring."
  [analysis-map]
  (let [job-uuid  (:uuid analysis-map)
        job-dir   (str "iplant-de-jobs/" (:username analysis-map) "/" job-uuid)
        irods-cfg (irods-config-path analysis-map)]
    (str 
     "#!/bin/bash\n"
     "readonly IPLANT_USER=" (:username analysis-map) "\n"
     "export IPLANT_USER\n"
     "cd ~\n"
     fail-script
     "mkdir -p " job-dir "\n"
     fail-script
     "pushd " job-dir "\n"
     fail-script
     "mkdir -p logs\n"
     fail-script
     "EXITSTATUS=0\n"
     (join "\n" (map script-line (jobs-in-order analysis-map)))
     "popd\n"
     "rm -r " job-dir "\n"
     "exit $EXITSTATUS\n")))

(defn create-submission-directory
  "Creates the local directory where the iplant.sh and iplant.cmd files get
   written out to."
  [{script-dir :working_dir :as analysis-map}]
  (let [dag-log-dir (ut/path-join script-dir "logs")]                     
    (log/info (str "Creating submission directories: " dag-log-dir))
    (if-not (.mkdirs (File. dag-log-dir))
      (log/warn (str "Failed to create directory: " dag-log-dir)))
    analysis-map))

(defn local-log-dir
  "Create the path to the local directory containing the condor logs."
  [{condor-log :condor-log-dir}]
  (ut/path-join condor-log "logs"))

(defn create-local-log-directory
  "Actually creates the local directory containing the condor logs."
  [analysis-map]
  (let [local-logs (local-log-dir analysis-map)]
    (log/info (str "Creating the local log directory: " local-logs))
    (if-not (.mkdirs (File. local-logs))
      (log/warn (str "Failed to create directory " local-logs)))
    analysis-map))

(defn scriptpath
  "Generates the path to the iplant.sh file."
  [analysis-map]
  (ut/path-join (:working_dir analysis-map) "logs" "iplant.sh"))

(defn script-command-file
  "Generates the path to the iplant.cmd file."
  [analysis-map]
  (ut/path-join (:working_dir analysis-map) "logs" "iplant.cmd"))

(defn generate-script-submission
  "Generates and writes out the iplant.sh and iplant.cmd files."
  [analysis-map]
  (spit (scriptpath analysis-map) (script analysis-map))
  (spit (irods-config-path analysis-map) (irods-config))
  (spit
   (script-command-file analysis-map)
   (script-submission
    (merge
     analysis-map
     {:script-path (scriptpath analysis-map)
      :local-log-dir (local-log-dir analysis-map)})))
  analysis-map)

(defn cleanup-analysis-map
  "Cleans up the analysis map."
  [analysis-map]
  (-> analysis-map
      (dissoc :steps)
      (assoc :executable "/bin/bash"
             :args (scriptpath analysis-map)
             :status "Submitted"
             :output (script-output (:working_dir analysis-map))
             :error (script-error (:working_dir analysis-map))
             :log (local-log-dir analysis-map))))

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
      (create-local-log-directory)
      (generate-script-submission))
  [(script-command-file analysis-map)
   (cleanup-analysis-map analysis-map)])

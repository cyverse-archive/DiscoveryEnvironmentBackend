(ns jex.test.dagify
  (:use [jex.dagify] :reload)
  (:use midje.sweet))

(fact
 (script-output "/tmp/script-dir") => "/tmp/script-dir/logs/script-output.log")

(fact
 (script-error "/tmp/script-dir") => "/tmp/script-dir/logs/script-error.log")

(fact
 (script-log "/tmp/log-dir") => "/tmp/log-dir/script-condor-log")

(def testsubmap
  {:username "testuser"
   :uuid "testuuid"
   :working_dir "/tmp/script-dir"
   :script-path "/tmp/script-dir/logs/iplant.sh"
   :local-log-dir "/tmp/log-dir"
   :steps [{:executable "/tmp/execpath/execname"}]})

(fact
 (ipc-exe testsubmap) => "+IpcExe = \"execname\"\n"
 (ipc-exe-path testsubmap) => "+IpcExePath = \"/tmp/execpath\"\n")

(fact
 (script-submission testsubmap) =>
  (str
   "universe = vanilla\n"
   "executable = /bin/bash\n"
   "arguments = \"/tmp/script-dir/logs/iplant.sh\"\n"
   "output = /tmp/script-dir/logs/script-output.log\n"
   "error = /tmp/script-dir/logs/script-error.log\n"
   "log = /tmp/log-dir/script-condor-log\n"
   "+IpcUuid = \"testuuid\"\n"
   "+IpcJobId = \"generated_script\"\n"
   "+IpcUsername = \"testuser\"\n"
   "+IpcExe = \"execname\"\n"
   "+IpcExePath = \"/tmp/execpath\"\n"
   "should_transfer_files = NO\n"
   "notification = NEVER\n"
   "queue\n"))

(fact
 (jobs-in-order
  {:imkdir-job "imkdir"
   :all-input-jobs ["all-input-jobs"]
   :steps ["steps"]
   :final-output-job "final-output-job"}) =>
   ["all-input-jobs"
    "steps"
    "final-output-job"])

(fact
 (script-line
  {:environment "environment"
   :executable "executable"
   :arguments "arguments"
   :stderr "stderr"
   :stdout "stdout"}) =>
   (str
    "environment executable arguments 1> stdout 2> stderr\n"
    "if [ ! \"$?\" -eq \"0\" ]; then\n"
    "\tEXITSTATUS=1\n"
    "fi\n"))

(def test-analysis
  {:imkdir-job
   {:environment "imkdir-env"
    :executable "imkdir-exec"
    :arguments "imkdir-args"
    :stderr "imkdir-stderr"
    :stdout "imkdir-stdout"}
   :final-output-job
   {:environment "final-job-env"
    :executable "final-job-exec"
    :arguments "final-job-args"
    :stderr "final-job-stderr"
    :stdout "final-job-stdout"}
   :all-input-jobs
   [{:environment "input-1-env"
     :executable "input-1-exec"
     :arguments "input-1-args"
     :stderr "input-1-stderr"
     :stdout "input-1-stdout"}
    {:environment "input-2-env"
     :executable "input-2-exec"
     :arguments "input-2-args"
     :stderr "input-2-stderr"
     :stdout "input-2-stdout"}]
   :steps
   [{:environment "step-1-env"
     :executable "step-1-exec"
     :arguments "step-1-args"
     :stderr "step-1-stderr"
     :stdout "step-1-stdout"}
    {:environment "step-2-env"
     :executable "step-2-exec"
     :arguments "step-2-args"
     :stderr "step-2-stderr"
     :stdout "step-2-stdout"}]
   :uuid "testuuid"
   :username "testuser"})

(fact
 (script test-analysis) =>
 (str
  "#!/bin/bash\n"
  "readonly IPLANT_USER=testuser\n"
  "export IPLANT_USER\n"
  "cd ~\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n\tEXITSTATUS=1\n\texit $EXITSTATUS\nfi\n"
  "mkdir -p iplant-de-jobs/testuser/testuuid\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n\tEXITSTATUS=1\n\texit $EXITSTATUS\nfi\n"
  "pushd iplant-de-jobs/testuser/testuuid\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n\tEXITSTATUS=1\n\texit $EXITSTATUS\nfi\n"
  "mkdir -p logs\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n\tEXITSTATUS=1\n\texit $EXITSTATUS\nfi\n"
  "EXITSTATUS=0\n"
  "input-1-env input-1-exec input-1-args 1> input-1-stdout 2> input-1-stderr\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n"
  "\tEXITSTATUS=1\n"
  "fi\n\n"
  "input-2-env input-2-exec input-2-args 1> input-2-stdout 2> input-2-stderr\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n"
  "\tEXITSTATUS=1\n"
  "fi\n\n"
  "step-1-env step-1-exec step-1-args 1> step-1-stdout 2> step-1-stderr\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n"
  "\tEXITSTATUS=1\n"
  "fi\n\n"
  "step-2-env step-2-exec step-2-args 1> step-2-stdout 2> step-2-stderr\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n"
  "\tEXITSTATUS=1\n"
  "fi\n\n"
  "final-job-env final-job-exec final-job-args 1> final-job-stdout 2> final-job-stderr\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n"
  "\tEXITSTATUS=1\n"
  "fi\n"
  "popd\n"
  "rm -r iplant-de-jobs/testuser/testuuid\n"
  "exit $EXITSTATUS\n"))

(fact
 (local-log-dir {:condor-log-dir "/tmp/log"}) => "/tmp/log/logs")

(fact
 (scriptpath {:working_dir "/tmp/work"}) => "/tmp/work/logs/iplant.sh")

(fact
 (script-command-file {:working_dir "/tmp/work"}) =>
 "/tmp/work/logs/iplant.cmd")

(def a-map
  {:steps []
   :working_dir "/tmp/working_dir"
   :username "testuser"
   :uuid "testuuid"
   :condor-log-dir "/tmp/log"})

(fact
 (cleanup-analysis-map a-map) =>
 {:executable "/bin/bash"
  :args "/tmp/working_dir/logs/iplant.sh"
  :status "Submitted"
  :output "/tmp/working_dir/logs/script-output.log"
  :error "/tmp/working_dir/logs/script-error.log"
  :log "/tmp/log/logs"
  :username "testuser"
  :uuid "testuuid"
  :condor-log-dir "/tmp/log"
  :working_dir "/tmp/working_dir"})

(.mkdirs (java.io.File. "test/scratch/"))

(fact
 (create-submission-directory {:working_dir "test/scratch"}) =>
 {:working_dir "test/scratch"}

 (.exists (java.io.File. "test/scratch/logs")) => true?)
(.delete (java.io.File. "test/scratch/logs"))

(fact
 (create-local-log-directory {:condor-log-dir "test/scratch/condor-log"}) =>
 {:condor-log-dir "test/scratch/condor-log"}

 (.exists (java.io.File. "test/scratch/condor-log")) => true?)
(.delete (java.io.File. "test/scratch/condor-log"))

(def test-gen-map
  {:working_dir "test/scratch/"
   :condor-log-dir "test/scratch/"
   :username "testuser"
   :uuid "testuuid"
   :imkdir-job
   {:environment "imkdir-env"
    :executable "imkdir-exec"
    :arguments "imkdir-args"
    :stderr "imkdir-stderr"
    :stdout "imkdir-stdout"}
   :final-output-job
   {:environment "final-job-env"
    :executable "final-job-exec"
    :arguments "final-job-args"
    :stderr "final-job-stderr"
    :stdout "final-job-stdout"}
   :all-input-jobs
   [{:environment "input-1-env"
     :executable "input-1-exec"
     :arguments "input-1-args"
     :stderr "input-1-stderr"
     :stdout "input-1-stdout"}
    {:environment "input-2-env"
     :executable "input-2-exec"
     :arguments "input-2-args"
     :stderr "input-2-stderr"
     :stdout "input-2-stdout"}]
   :steps
   [{:environment "step-1-env"
     :executable "step-1-exec"
     :arguments "step-1-args"
     :stderr "step-1-stderr"
     :stdout "step-1-stdout"}
    {:environment "step-2-env"
     :executable "step-2-exec"
     :arguments "step-2-args"
     :stderr "step-2-stderr"
     :stdout "step-2-stdout"}]})

(.mkdirs (java.io.File. (str (:working_dir test-gen-map) "logs")))
(.mkdirs (java.io.File. (:condor-log-dir test-gen-map)))

(def result-gen-script
  (generate-script-submission test-gen-map))

(fact
 (.exists (java.io.File. "test/scratch/logs/iplant.cmd")) => true?
 (.exists (java.io.File. "test/scratch/logs/iplant.sh")) => true?

 (slurp "test/scratch/logs/iplant.cmd") =>
 "universe = vanilla\nexecutable = /bin/bash\narguments = \"test/scratch/logs/iplant.sh\"\noutput = test/scratch/logs/script-output.log\nerror = test/scratch/logs/script-error.log\nlog = test/scratch/logs/script-condor-log\n+IpcUuid = \"testuuid\"\n+IpcJobId = \"generated_script\"\n+IpcUsername = \"testuser\"\n+IpcExe = \"step-1-exec\"\n+IpcExePath = \"\"\nshould_transfer_files = NO\nnotification = NEVER\nqueue\n"

 (slurp "test/scratch/logs/iplant.sh") =>
 (str
  "#!/bin/bash\n"
  "readonly IPLANT_USER=testuser\n"
  "export IPLANT_USER\n"
  "cd ~\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n\tEXITSTATUS=1\n\texit $EXITSTATUS\nfi\n"
  "mkdir -p iplant-de-jobs/testuser/testuuid\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n\tEXITSTATUS=1\n\texit $EXITSTATUS\nfi\n"
  "pushd iplant-de-jobs/testuser/testuuid\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n\tEXITSTATUS=1\n\texit $EXITSTATUS\nfi\n"
  "mkdir -p logs\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n\tEXITSTATUS=1\n\texit $EXITSTATUS\nfi\n"
  "EXITSTATUS=0\n"
  "input-1-env input-1-exec input-1-args 1> input-1-stdout 2> input-1-stderr\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n"
  "\tEXITSTATUS=1\n"
  "fi\n\n"
  "input-2-env input-2-exec input-2-args 1> input-2-stdout 2> input-2-stderr\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n"
  "\tEXITSTATUS=1\n"
  "fi\n\n"
  "step-1-env step-1-exec step-1-args 1> step-1-stdout 2> step-1-stderr\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n"
  "\tEXITSTATUS=1\n"
  "fi\n\n"
  "step-2-env step-2-exec step-2-args 1> step-2-stdout 2> step-2-stderr\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n"
  "\tEXITSTATUS=1\n"
  "fi\n\n"
  "final-job-env final-job-exec final-job-args 1> final-job-stdout 2> final-job-stderr\n"
  "if [ ! \"$?\" -eq \"0\" ]; then\n"
  "\tEXITSTATUS=1\n"
  "fi\n"
  "popd\n"
  "rm -r iplant-de-jobs/testuser/testuuid\n"
  "exit $EXITSTATUS\n"))

(.delete (java.io.File. "test/scratch/logs/iplant.cmd"))
(.delete (java.io.File. "test/scratch/logs/iplant.sh"))
(.delete (java.io.File. "test/scratch/logs"))
(.delete (java.io.File. "test/scratch"))

(def test-dagify-result (dagify test-gen-map))

(fact
 (.exists (java.io.File. "test/scratch")) => true?
 (.exists (java.io.File. "test/scratch/logs")) => true?
 (.exists (java.io.File. "test/scratch/logs/iplant.cmd")) => true?
 (.exists (java.io.File. "test/scratch/logs/iplant.sh")) => true?
 (count test-dagify-result) => 2
 (first test-dagify-result) => "test/scratch/logs/iplant.cmd"
 (:steps (last test-dagify-result)) => nil?
 (:executable (last test-dagify-result)) => "/bin/bash"
 (:args (last test-dagify-result)) => "test/scratch/logs/iplant.sh"
 (:status (last test-dagify-result)) => "Submitted"
 (:output (last test-dagify-result)) => "test/scratch/logs/script-output.log"
 (:error (last test-dagify-result)) => "test/scratch/logs/script-error.log"
 (:log (last test-dagify-result)) => "test/scratch/logs")

(.delete (java.io.File. "test/scratch/logs/iplant.cmd"))
(.delete (java.io.File. "test/scratch/logs/iplant.sh"))
(.delete (java.io.File. "test/scratch/logs"))
(.delete (java.io.File. "test/scratch/condor-log/logs/"))
(.delete (java.io.File. "test/scratch/condor-log"))
(.delete (java.io.File. "test/scratch"))

(ns jex.incoming-xforms
  (:use [jex.common])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ut]
            [jex.config :as cfg]))

(def replacer
  "Params: [regex replace-str str-to-modify]."
  #(.replaceAll (re-matcher %1 %3) %2))

(def replace-at
  "Replaces @ sign in a string. [replace-str str-to-modify"
   (partial replacer #"@"))

(def at-underscore
  "Replaces @ sign with _. [str-to-modify]"
  (partial replace-at "_"))

(def replace-space
  "Replaces space. [replace-str str-to-modify]"
   (partial replacer #"\s"))

(def space-underscore
  "Replaces spaces with underscores. [str-to-modify]"
   (partial replace-space "_"))

(def now-fmt
  "Date format used in directory and file names."
   "yyyy-MM-dd-HH-mm-ss.SSS")

(defn fmt-date
  "Translates date-obj into the format specified by format-str."
  [format-str date-obj]
  (. (java.text.SimpleDateFormat. format-str) format date-obj))

(defn date
  "Returns the current date as a java.util.Date instance."
  []
  (java.util.Date.))

(defn analysis-dirname
  "Creates a directory name for an analysis. Used when the submission
   doesn't specify an output directory.  Some types of jobs, for example
   Foundational API jobs, include a timestamp in the job name, so a timestamp
   will not be appended to teh directory name in those cases."
  [analysis-name date-str]
  (if-not (re-find #"-\d{4}(?:-\d{2}){5}\.\d+$" analysis-name)
    (str analysis-name "-" date-str)
    analysis-name))

(defn now-date
  "Adds a key to condor-map called :now_date that's formatted like now-fmt."
  ([condor-map]
    (now-date condor-map date))
  ([condor-map date-func]
    (assoc condor-map :now_date (fmt-date now-fmt (date-func)))))

(defn pathize
  "Makes a string safe for inclusion in a path by replacing @ and spaces with
   underscores."
  [p]
  (-> p at-underscore space-underscore))

(defn irods-config
  [{working-dir :working_dir}]
  (ut/path-join working-dir "logs/irods-config"))

(defn analysis-attrs
  "Adds some basic top-level keys to condor-map that are needed for subsequent
   tranformations."
  ([condor-map]
     (analysis-attrs condor-map date))
  ([condor-map date-func]
     (assoc condor-map
       :run-on-nfs (cfg/run-on-nfs)
       :type (or (:type condor-map) "analysis")
       :username (pathize (:username condor-map))
       :nfs_base (cfg/nfs-base)
       :irods_base (cfg/irods-base)
       :submission_date (.getTime (date-func)))))

(defn output-directory
  "Returns a string containing iRODS output directory based on settings
   condor-map. Does not actually associate the value with :output_dir in
   condor-map."
  [condor-map]
  (let [output-dir    (:output_dir condor-map)
        create-subdir (:create_output_subdir condor-map)
        irods-base    (:irods_base condor-map)
        username      (:username condor-map)
        analysis-dir  (analysis-dirname
                       (pathize (:name condor-map))
                       (:now_date condor-map))]
    (cond
     (or (nil? output-dir)
         (nil? create-subdir))
     (ut/rm-last-slash
      (ut/path-join irods-base username "analyses" analysis-dir))

     (and
      (string/blank? output-dir)
      create-subdir)
     (ut/rm-last-slash
      (ut/path-join irods-base username "analyses" analysis-dir))

     (and
      (string/blank? output-dir)
      (false? create-subdir))
     (ut/rm-last-slash
      (ut/path-join irods-base username "analyses" analysis-dir))

     (and
      (not (string/blank? output-dir))
      create-subdir)
     (ut/rm-last-slash
      (ut/path-join output-dir analysis-dir))

      (and (not (string/blank? output-dir)) (false? create-subdir))
      (ut/rm-last-slash output-dir)

      :else
      (ut/rm-last-slash
       (ut/path-join irods-base username "analyses" analysis-dir)))))

(defn file-metadata-arg
  [meta-seq]
  (let [args (atom "")]
    (doseq [m meta-seq]
      (reset! args (str @args (str " -m '" (string/join "," [(:attr m) (:value m) (:unit m)]) "'"))))
    @args))

(defn context-dirs
  "Adds the :output_dir :working_dir and :condor-log-dir keys to the condor-map.
   These values are calculated using values that were added by (analysis-attrs)."
  [condor-map]
  (let [username     (:username condor-map)
        nfs-base     (:nfs_base condor-map)
        analysis-dir (analysis-dirname
                      (pathize (:name condor-map))
                      (:now_date condor-map))
        log-dir (ut/add-trailing-slash
                      (ut/path-join
                       (cfg/condor-log-path)
                       username
                       analysis-dir))
        output-dir   (output-directory condor-map)
        working-dir  (ut/add-trailing-slash
                      (ut/path-join nfs-base username analysis-dir))]
    (assoc condor-map
           :output_dir output-dir
           :working_dir working-dir
           :condor-log-dir log-dir)))

(defn param-maps
  "This looks goofy, but it filters out unneeded crap from the params."
  [params]
  (for [param params]
    {:name  (:name param)
     :value (:value param)
     :order (:order param)}))

(defn naively-quote
  "Naievely single-quotes a string that will be placed on the command line
   using plain string substitution.  This works, but may leave extra pairs
   of leading or trailing quotes if there was a leading or trailing quote
   in the original string, which is valid, but may be confusing to human
   readers."
  [value]
  (str \' (string/replace value "'" "'\\''") \'))

(defn quote-value
  "Quotes and escapes a string that is supposed to be passed in to a tool on
   the command line."
  [value]
  (-> value
    naively-quote
    (string/replace #"^''|''$" "")))

(defn escape-params
  "Escapes the spaces in the params list."
  [params]
  (string/join " "
    (flatten
      (map
        #(vector (:name %1) (quote-value (:value %1)))
        (sort-by :order params)))))

(defn containerized?
  "Returns true if the step should run in a container."
  [step-map]
  (contains? (:component step-map) :container))

(defn container-info
  "Returns a map of the container information extracted from the step map."
  [step-map]
  (get-in step-map [:component :container]))

(defn data-containers
  "Returns a list of the data containers associated with the job."
  [condor-map]
  (assoc condor-map :data-containers
         (distinct (flatten (mapv (fn [step]
                   (mapv #(select-keys %1 [:tag :name :name_prefix :host_path :container_path :read_only])
                         (get-in step [:component :container :container_volumes_from])))
                 (:steps condor-map))))))

(defn container-volumes?
  "Returns true if the container has volumes defined."
  [container-map]
  (contains? container-map :container_volumes))

(defn container-devices?
  "Returns true if the container has devices defined."
  [container-map]
  (contains? container-map :container_devices))

(defn container-volumes-from?
  "Returns true if the container imports volumes from another container."
  [container-map]
  (contains? container-map :container_volumes_from))

(defn container-working-directory
  [container-map]
  (if (contains? container-map :working_directory)
    (:working_directory container-map)
    "/de-app-work"))

(defn container-working-directory-arg
  [container-map]
  (let [wd (container-working-directory container-map)]
    (str "-w " wd)))

(defn container-volume-args
  [container-map]
  (str "-v $(pwd):" (container-working-directory container-map) " "
       (if (container-volumes? container-map)
         (string/join " " (map
                           (fn [volume]
                             (str "-v " (:host_path volume) ":" (:container_path volume)))
                           (:container_volumes container-map)))
         "")))

(defn container-device-args
  [container-map]
  (if (container-devices? container-map)
    (string/join " " (map
                      (fn [device]
                        (str "--device=" (:host_path device) ":" (:container_path device)))
                      (:container_devices container-map)))))

(defn container-volumes-from-args
  [uuid container-map]
  (if (container-volumes-from? container-map)
    (string/join " " (map
                      (fn [vf]
                        (str "--volumes-from=" (volumes-from-name uuid (:name_prefix vf))))
                      (:container_volumes_from container-map)))))

(defn container-name-arg
  [container-map]
  (if (contains? container-map :name)
    (str "--name " (:name container-map))))

(defn container-network-mode-arg
  [container-map]
  (if (contains? container-map :network_mode)
    (str "--net=" (:network_mode container-map))
    "--net=bridge"))

(defn container-cpu-shares-arg
  [container-map]
  (if (contains? container-map :cpu_shares)
    (str "--cpu-shares=" (:cpu_shares container-map))))

(defn container-memory-limit-arg
  [container-map]
  (if (contains? container-map :memory_limit)
    (str "--memory=" (:memory_limit container-map) "b")))

(defn- de-image?
  [container-map]
  (contains?
    #{"discoenv/porklock" "discoenv/curl-wrapper" "gims.iplantcollaborative.org:5000/backwards-compat"}
    (:name (:image container-map))))

(defn container-image-tag
  [container-map]
  (cond
    (de-image? container-map)               (str ":" (cfg/porklock-tag))
    (contains? (:image container-map) :tag) (str ":" (:tag (:image container-map)))
    :else                                   ""))

(defn container-image-arg
  [container-map]
  (let [image (:image container-map)
        tag   (container-image-tag container-map)]
    (str (:name image) tag)))

(defn container-entrypoint-arg
  [container-map]
  (if (and (contains? container-map :entrypoint)
           (not (string/blank? (:entrypoint container-map))))
    (str "--entrypoint=\"" (:entrypoint container-map) "\"")))

(defn container-env-args
  [step-map]
  (if (contains? step-map :environment)
    (string/join " " (map
                      (fn [[k v]]
                        (str "--env=\"" k "=" v "\""))
                      (seq (:environment step-map))))))

(defn backwards-compatible?
  "Returns true if the docker image is 'centos:5' and the path to the
   executable contains either /usr/local2 or /usr/local3. Yeah, it's
   weird."
  [step-map]
  (let [loc (get-in step-map [:component :location])
        img (container-image-arg (container-info step-map))]
    (.startsWith img "gims.iplantcollaborative.org:5000/backwards-compat")))

(defn backwards-compatible-executable
  [step-map]
  (if (backwards-compatible? step-map)
    (ut/path-join
     (get-in step-map [:component :location])
     (get-in step-map [:component :name]))
    ""))

(defn backwards-compatible-args
  [step-map]
  (if (backwards-compatible? step-map)
    (str "-v /usr/local2/:/usr/local2 -v /usr/local3/:/usr/local3/ -v /data2/:/data2/")))

(defn container-args
  [uuid step-map]
  (let [container-map (container-info step-map)]
    (string/join
     " "
     (filter
      (comp not nil?)
      ["run --rm -a stdout -a stderr -e IPLANT_USER -e IPLANT_EXECUTION_ID"
       (backwards-compatible-args step-map)
       (container-volume-args container-map)
       (container-device-args container-map)
       (container-volumes-from-args uuid container-map)
       (container-name-arg container-map)
       (container-working-directory-arg container-map)
       (container-name-arg container-map)
       (container-memory-limit-arg container-map)
       (container-cpu-shares-arg container-map)
       (container-network-mode-arg container-map)
       (container-env-args step-map)
       (container-entrypoint-arg container-map)
       (container-image-arg container-map)
       (backwards-compatible-executable step-map)]))))

(defn container-image-string
  "Returns the string that can be passed to docker pull for the given step."
  [step-map]
  (let [container-map (container-info step-map)]
    (container-image-arg container-map)))

(defn executable
  "Takes in a step map and returns the executable path. This will be the full
   path to the executable since they're the same across all of the Condor
   nodes."
  [step-map]
  "docker")

(defn arguments
  "Takes in a step map map and returns the formatted arguments
   for that step in the analysis."
  [uuid step-map]
  (let [cmd (-> (get-in step-map [:config :params]) param-maps escape-params)]
    (str (container-args uuid step-map) " " cmd)))

(defn stdin
  "Returns the path to the stdin file or nil if there isn't one. This should
   be relative to the current working directory since it's used out on the
   Condor cluster."
  [step-map]
  (if (contains? step-map :stdin)
    (quote-value (:stdin step-map))
    nil))

(defn stdout
  "Returns the path to the stdout file or nil if there isn't one. This should
   be relative to the current working directory since it's used out on the
   Condor cluster."
  [step-map index]
  (if (contains? step-map :stdout)
    (quote-value (:stdout step-map))
    (str "logs/" "condor-stdout-" index)))

(defn stderr
  "Returns the path to the stderr file or nil if there isn't one. This should
   be relative to the current working directory since it's used out on the
   Condor cluster."
  [step-map index]
  (if (contains? step-map :stderr)
    (quote-value (:stderr step-map))
    (str "logs/" "condor-stderr-" index)))

(defn log-file
  "Returns the path to the condor log files."
  [step-map index condor-log]
  (if (contains? step-map :log-file)
    (ut/path-join condor-log (:log-file step-map))
    (ut/path-join condor-log "logs" (str "condor-log-" index))))

(defn step-iterator-vec
  "Returns a vector of vectors that look like the following:

   [[0 {:this \"is a step\"}]]

   The integer is the step index in the list of steps and the map is the actual
   step map that corresponds to the index in the list of steps in the analysis."
  [condor-map]
  (mapv vector (iterate inc 0) (:steps condor-map)))

(defn process-steps
  "Iterates over the steps in the analysis and morphs them into something
   that can be used to generate a shell script. The result will be a sequence
   of transformed step maps."
  [condor-map]
  (for [[step-idx step] (step-iterator-vec condor-map)]
    (assoc step
      :id              (str "condor-" step-idx)
      :type            "condor"
      :submission_date (:submission_date condor-map)
      :status          "Submitted"
      :executable      (executable step)
      :arguments       (arguments (:uuid condor-map) step)
      :stdout          (stdout step step-idx)
      :stderr          (stderr step step-idx)
      :log-file        (log-file step step-idx (:condor-log-dir condor-map)))))

(defn set-container-images
  "Adds a top-level :container-images key to the condor-map with a set of
   image names associated with it."
   [condor-map]
   (assoc condor-map :container-images
     (into []
       (for [[_ step] (step-iterator-vec condor-map)]
         (container-image-string step)))))

(defn steps
  "Processes the steps in a map into a saner format. Returns a new version
   of condor-map with the steps modified to be easier to use."
  [condor-map]
  (assoc condor-map :steps (process-steps condor-map)))

(defn handle-source-path
  "Takes in a source path and a multiplicity and adds a trailing slash if
   needed."
  [source-path multiplicity]
  (if (= multiplicity "collection")
    (ut/add-trailing-slash source-path)
    source-path))

(defn input-id-str
  "Takes in the step index and input index and returns the input job
   identifier. The identifier is used to put together filenames for logs
   associated with the input job in question."
  [step-index input-index]
  (str "condor-" step-index "-input-" input-index))

(defn input-executable
  [step-map]
  "docker")

(defn input-stdout
  "Takes in the step index and the input index and returns the path to the
   stdout file for an input job. This should always end up being a relative path
   since the stdout file is created out on the Condor cluster."
  [step-index input-index]
  (str "logs/" (input-id-str step-index input-index) "-stdout"))

(defn input-stderr
  "Takes in the step index and the input index and returns the path to the
   stderr file for an input job. This should always end up being a relative path
   since the stderr file is created out on the Condor cluster."
  [step-index input-index]
  (str "logs/" (input-id-str step-index input-index) "-stderr"))

(defn input-log-file
  "Pieces together the path to the condor log file."
  [condor-log step-index input-index]
  (ut/path-join
   condor-log
   "logs"
   (str (input-id-str step-index input-index) "-log")))

(defn input-arguments
  "Formats the arguments to porklock for an input job."
  [condor-map step-map source input-map]
  (let [file-metadata (or (:file-metadata condor-map) [])
        arg-prefix    (str "run --rm -a stdout -a stderr -v $(pwd):/de-app-work -w /de-app-work discoenv/porklock:" (cfg/porklock-tag))]
    (str arg-prefix
         " get --user " (:username condor-map)
         " --source " (quote-value
                       (handle-source-path source (:multiplicity input-map)))
         " --config irods-config"
         (file-metadata-arg file-metadata))))

(defn input-iterator-vec
  "Returns a vector of vectors that make iterating over the input jobs in a
   step easier to handle. The return value looks like this:

   [[0 {:input :map}]]

   The integer is the index into the input list in the config object of each
   step. The map is the entry in the input list that corresponds with the
   index."
  [step-map]
  (map vector (iterate inc 0) (get-in step-map [:config :input])))

(defn process-step-inputs
  "Iterators over the input jobs for a step in the analysis and transforms them
   into a new map that can be more easily used to create bash scripts. Returns
   a seq of transformed input maps."
  [condor-map [step-idx step-map]]
  (for [[input-idx input] (input-iterator-vec step-map)]
    {:id              (input-id-str step-idx input-idx)
     :submission_date (:submission_date condor-map)
     :type            "condor"
     :status          "Submitted"
     :retain          (:retain input)
     :multi           (:multiplicity input)
     :source          (:value input)
     :executable      (input-executable step-map)
     :arguments       (input-arguments
                       condor-map
                       step-map
                       (:value input)
                       input)
     :stdout          (input-stdout step-idx input-idx)
     :stderr          (input-stderr step-idx input-idx)
     :log-file        (input-log-file
                       (:condor-log-dir condor-map)
                       step-idx
                       input-idx)}))

(defn process-inputs
  "Iterates over the steps in the analysis map and then over the inputs for
   each step and transforms each input map into something easier to use.
   Returns a seq of transformed steps."
  [condor-map]
  (for [step-iter (step-iterator-vec condor-map)]
    (assoc (last step-iter)
      :input-jobs (process-step-inputs condor-map step-iter))))

(defn input-jobs
  "Adds input job definitions to the incoming analysis map."
  [condor-map]
  (assoc condor-map :steps (process-inputs condor-map)))

(defn output-arguments
  "Formats the porklock arguments for output jobs."
  [step-map user source dest]
  (let [arg-prefix (str "run --rm -a stdout -a stderr -v $(pwd):/de-app-work -w /de-app-work discoenv/porklock:" (cfg/porklock-tag))]
    (str arg-prefix
         " put --user " user
         " --source " (quote-value source)
         " --destination " (quote-value dest)
         " --config logs/irods-config")))

(defn output-executable [step-map] "docker")

(defn output-id-str
  "Generates an identifier for output jobs based on the step index and the
   output job index. This is used to generate filenames for logs associated
   with output jobs."
  [step-index output-index]
  (str "condor-" step-index "-output-" output-index))

(defn output-iterator-vec
  "Returns a vector of vectors that make iterating over the output jobs in a
   step easier to handle. The return value looks like this:

   [[0 {:input :map}]]

   The integer is the index into the output list in the config object of each
   step. The map is the entry in the output list that corresponds with the
   index."
  [step-map]
  (map vector (iterate inc 0) (get-in step-map [:config :output])))

(defn process-step-outputs
  "Iterates over the outputs associated with a step and transforms them into
   something we can use to generate a bash script. Returns a list of modified
   output maps."
  [condor-map [step-idx step-map]]
  (for [[output-idx output] (output-iterator-vec step-map)]
    {:id              (output-id-str step-idx output-idx)
     :type            "condor"
     :status          "Submitted"
     :submission_date (:submission_date condor-map)
     :retain          (:retain output)
     :multi           (:multiplicity output)
     :executable      (output-executable step-map)
     :arguments       (output-arguments
                       step-map
                       (:username condor-map)
                       (:name output)
                       (:output_dir condor-map))
     :source          (:name output)
     :dest            (:output_dir condor-map)}))

(defn process-outputs
  "Iterates over the output-jobs associated with each step, modifies them via
   (process-step-outputs) and associates them with the key :output-jobs in each
   map."
  [condor-map]
  (for [step-iter (step-iterator-vec condor-map)]
    (assoc (last step-iter)
      :output-jobs (process-step-outputs condor-map step-iter))))

(defn output-jobs
  "Adds output job definitions to the incoming analysis map.

   condor-map must have the following key-values before calling:
         :output_dir :working_dir

   The result of this function is a map in each step called :output-jobs
   with the following format:
       {:id String
        :source String
        :dest   String}
  "
  [condor-map]
  (assoc condor-map :steps (process-outputs condor-map)))

(defn all-input-jobs
  "Adds the :all-input-jobs key to condor-map. It's a list of all of the input
   jobs in the submission, extracted from the :steps list."
  [condor-map]
  (assoc condor-map
    :all-input-jobs (apply concat (map :input-jobs (:steps condor-map)))))

(defn all-output-jobs
  "Adds the :all-output-jobs key to condor-map. It's a list of all of the output
   jobs in the submission, extracted from the :steps list."
  [condor-map]
  (assoc condor-map
    :all-output-jobs (apply concat (map :output-jobs (:steps condor-map)))))

(defn input-coll [jdef]
  "Examines an input job definition and returns the path to file or directory."
  (quote-value
    (let [multi (:multi jdef)
          fpath (ut/basename (:source jdef))]
      (if (= multi "collection") (ut/add-trailing-slash fpath) fpath))))

(defn make-abs-output
  "Takes in an output path and makes it absolute if it's not. Note that
   this is intended for use in a bash script and will get executed on
   the Condor cluster."
  [out-path]
  (if (not (. out-path startsWith "/"))
    (str "$(pwd)/" (quote-value out-path))
    (quote-value out-path)))

(defn output-coll
  "Examines an output job definition and returns the path to the file or
   directory."
  [jdef]
  (if (= (:multi jdef) "collection")
    (make-abs-output (ut/add-trailing-slash (:source jdef)))
    (:source jdef)))

(defn- all-paths
  [inputs outputs archive-logs?]
  (let [paths (conj inputs outputs (cfg/filter-files))]
    (flatten
      (if archive-logs?
        paths
        (conj paths ["logs"])))))

(defn exclude-arg
  "Formats the -exclude option for the filetool jobs based on the input and
   output job definitions."
  [{inputs :all-input-jobs outputs :all-output-jobs :as condor-map}]
  (log/info "exclude-arg")
  (log/info (str "COUNT INPUTS: " (count inputs)))
  (log/info (str "COUNT OUTPUTS: " (count outputs)))
  (let [archive-logs? (:archive_logs condor-map true)
        not-retain    (comp not :retain)
        input-paths   (map input-coll (filter not-retain inputs))
        output-paths  (map output-coll (filter not-retain outputs))
        all-paths     (all-paths input-paths output-paths archive-logs?)]
    (if (pos? (count all-paths))
      (str "--exclude " (string/join "," all-paths))
      "")))

(defn meta-analysis-id
  [{analysis-id :app_id :as condor-map}]
  (if-not (nil? analysis-id)
    (assoc condor-map :file-metadata
           (conj (:file-metadata condor-map)
                 {:attr  "ipc-analysis-id"
                  :value analysis-id
                  :unit  "UUID"}))
    condor-map))

(defn meta-app-execution
  [{uuid :uuid :as condor-map}]
  (if-not (nil? uuid)
    (assoc condor-map :file-metadata
           (conj (:file-metadata condor-map)
                 {:attr "ipc-execution-id"
                  :value uuid
                  :unit "UUID"}))
    condor-map))

(defn add-analysis-metadata
  [{analysis-id :app_id uuid :uuid :as condor-map}]
  (-> condor-map meta-analysis-id meta-app-execution))

(defn shotgun-executable [condor-map] "docker")

(defn shotgun-arguments
  [{output-dir    :output_dir
    cinput-jobs   :all-input-jobs
    coutput-jobs  :all-output-jobs
    username      :username
    file-metadata :file-metadata
    :or {file-metadata []}
    :as condor-map}]
  (let [arg-prefix (str "run --rm -a stdout -a stderr -v $(pwd):/de-app-work -w /de-app-work discoenv/porklock:" (cfg/porklock-tag))]
    (str arg-prefix
         " put --user " username
         " --config irods-config"
         " --destination " (quote-value output-dir)
         (if (:skip-parent-meta condor-map) " --skip-parent-meta" "")
         (file-metadata-arg file-metadata)
         " "
         (exclude-arg condor-map))))

(defn shotgun-job-map
  "Formats a job definition for the output job that transfers
   all of the files back into iRODS after the analysis is complete."
  [{output-dir    :output_dir
    condor-log    :condor-log-dir
    cinput-jobs   :all-input-jobs
    coutput-jobs  :all-output-jobs
    username      :username
    file-metadata :file-metadata
    :or {file-metadata []}
    :as condor-map}]
  (log/info "shotgun-job-map")
  {:id          "output-last"
   :status      "Submitted"
   :executable  (shotgun-executable condor-map)
   :stderr      "logs/output-last-stderr"
   :stdout      "logs/output-last-stdout"
   :log-file    "logs/output-last-log"
   :arguments   (shotgun-arguments condor-map)})

(defn extra-jobs
  "Associates the :final-output-job definition
   with condor-map. Returns a new version of condor-map."
  [condor-map]
  (assoc condor-map
    :final-output-job
    (shotgun-job-map condor-map)))

(defn rm-step-component
  "Removes the :component key-value pair from each step in condor-map.
   Returns a new version of condor-map."
  [condor-map]
  (assoc condor-map
    :steps (for [step (:steps condor-map)]
             (dissoc step :component))))

(defn rm-step-config
  "Removes the :config key-value pair from each step in condor-map.
   Returns a new version of condor-map."
  [condor-map]
  (assoc condor-map
    :steps (for [step (:steps condor-map)]
             (dissoc step :config))))

(defn log-thru
  [condor-map]
  (clojure.pprint/pprint condor-map)
  condor-map)

(defn transform
  "Transforms the condor-map that's passed in into something more useable."
  ([condor-map]
     (transform condor-map date))
  ([condor-map date-func]
     (-> condor-map
         log-thru
         (now-date date-func)
         (analysis-attrs date-func)
         context-dirs
         add-analysis-metadata
         set-container-images
         steps
         data-containers
         input-jobs
         output-jobs
         all-input-jobs
         all-output-jobs
         extra-jobs
         #_(rm-step-component)
         rm-step-config)))

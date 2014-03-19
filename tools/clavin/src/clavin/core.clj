(ns clavin.core
  (:gen-class)
  (:use [clojure.java.io :only [file]])
  (:require [clojure.tools.cli :as cli]
            [clavin.environments :as env]
            [clavin.generator :as gen]
            [clavin.get-props :as gp]
            [clavin.loader :as loader]
            [clavin.templates :as ct]
            [clavin.zk :as zk]
            [clojure-commons.file-utils :as ft]
            [clojure.string :as string]
            [clojure-commons.props :as ccprops]))

(def ^:private default-zkhosts-path
  "/etc/iplant-services/zkhosts.properties")

(declare main-help)

(defn- to-integer
  [v]
  (Integer. v))

(defn parse-args
  "Parses the arguments for the 'props' subcommand."
  [args]
  (cli/cli
   args
   ["-h" "--help" "Show help." :default false :flag true]
   ["-f" "--envs-file" "The file containing the environment definitions."
    :default nil]
   ["-t" "--template-dir" "The directory containing the templates."
    :default nil]
   ["--host" "The Zookeeper host to connect to." :default nil]
   ["--port" "The Zookeeper client port to connect to." :default 31381
    :parse-fn to-integer]
   ["-z" "--zkhosts-path"
    "The path to the file containing the Zookeeper connection settings."
    :default default-zkhosts-path]
   ["--acl"  "The file containing Zookeeper hostname ACLs." :default nil]
   ["-a" "--app" "The application the settings are for." :default "de"]
   ["-e" "--env" "The environment that the options should be entered into."
    :default nil]
   ["-d" "--deployment"
    "The deployment inside the environment that is being configured."
    :default nil]))

(def ^:private required-args
  [:envs-file :template-dir :acl :deployment])

(defn parse-get-props-args
  "Parses the arguments for the 'get-props' subcommand."
  [args]
  (cli/cli
   args
   ["-h" "--help" "Show help." :default false :flag true]
   ["--host" "The Zookeeper host to connect to." :default nil]
   ["--port" "The Zookeeper port to connect to." :default 31381
    :parse-fn to-integer]
   ["-z" "--zkhosts-path"
    "The path to the file containing the Zookeeper connection settings."
    :default default-zkhosts-path]
   ["-s" "--service" "The service to get the settings for." :default nil]
   ["--service-host" "The host that the service is running on." :default nil]))

(def ^:private required-get-props-args
  [:service])

(defn parse-files-args
  "Parses the arguments for the 'files' subcommand."
  [args]
  (cli/cli
   args
   ["-h" "--help" "Show help." :default false :flag true]
   ["-f" "--envs-file" "The file containing the environment definitions."
    :default nil]
   ["-t" "--template-dir" "The directory containing the templates."
    :default nil]
   ["-a" "--app" "The application the settings are for." :default "de"]
   ["-e" "--env" "The environment that the options are for." :default nil]
   ["-d" "--deployment" "The deployment that the properties files are for."
    :default nil]
   ["--dest" "The destination directory for the files." :default nil]))

(def ^:private required-files-args
  [:envs-file :template-dir :deployment :dest])

(defn parse-hosts-args
  "Parses the arguments for the 'hosts' subcommand."
  [args]
  (cli/cli
   args
   ["-h" "--help" "Show help." :default false :flag true]
   ["--acl"  "The file containing Zookeeper hostname ACLs." :default nil]
   ["--host" "The Zookeeper host to connection to." :default nil]
   ["--port" "The Zookeeper client port to connection to." :default 31381
    :parse-fn to-integer]
   ["-z" "--zkhosts-path"
    "The path to the file containing the Zookeeper connection settings."
    :default default-zkhosts-path]))

(def ^:private required-hosts-args
  [:acl])

(defn parse-envs-args
  "Parses the arguments for the 'envs' subcommand."
  [args]
  (cli/cli
   args
   ["-h" "--help" "Show help." :default false :flag true]
   ["-l" "--list" "List environments." :default false :flag true]
   ["-v" "--validate" "Validate the environments file." :default false
    :flag true]
   ["-f" "--envs-file" "The file containing the environment definitions."
    :default nil]))

(def ^:private required-envs-args
  [[:list :validate] :envs-file])

(defn parse-templates-args
  "Parses the arguments for the 'templates' subcommand."
  [args]
  (cli/cli
   args
   ["-h" "--help" "Show help." :default false :flag true]
   ["-l" "--list" "List templates." :default false :flag true]
   ["-v" "--validate" "Validate templates." :default false :flag true]
   ["-t" "--template-dir" "The directory containing the templates."
    :default nil]
   ["-f" "--envs-file" "The file containing the environment definitions."
    :default nil]))

(def ^:private required-templates-args
  [[:list :validate] :template-dir])

(defn keyword->opt-name
  "Converts a keyword to an option name."
  [k]
  (str "--" (name k)))

(defn get-directory
  "Gets the path to a directory from a command-line option.  The path must
   exist and refer to an extant directory if the command-line option is
   specified.  If the path does not exist or does not refer to an extant
   directory then an error message will be displayed and the program will exit."
  [opts help-str opt-k]
  (let [v (opts opt-k)]
    (when-not (or (nil? v) (ft/dir? v))
      (println (keyword->opt-name opt-k) "must refer to a directory.")
      (println help-str)
      (System/exit 1))
    v))

(defn get-regular-file
  "Gets the path to a regular file from a command-line option.  The path must
   exist and refer to an extant file if the command-line option is specified.
   If the path does not exist or does not refer to a regular file then an error
   message will be displayed and the program will exit."
  [opts help-str opt-k]
  (let [v (opts opt-k)]
    (when-not (or (nil? v) (ft/file? v))
      (println (keyword->opt-name opt-k) "must refer to a regular file.")
      (println help-str)
      (System/exit 1))
    v))

(defn validate-single-opt
  "Validates a single required option.  The option is assumed to be defined if
   it contains any value that evaluates to true.  If the option contains a value
   that evauates to false then an error message will be displayed and the
   program will exit."
  [opts help-str opt-k]
  (when-not (opts opt-k)
    (println (keyword->opt-name opt-k) "is required.")
    (println help-str)
    (System/exit 1)))

(defn validate-multiple-opts
  "Validates multiple mutually exclusive options.  If none of the options
   contains a true value or more than one option contains a true value then
   an error message will be displayed and the program will exit."
  [opts help-str opt-ks]
  (let [defined-opts (filter opts opt-ks)
        opt-names    (string/join ", " (map keyword->opt-name opt-ks))]
    (when-not (< 0 (count defined-opts) 2)
      (println "please specify exactly one of" opt-names)
      (println help-str)
      (System/exit 1))))

(defn validate-opts
  "Verifies that all required command-line options are actually specified on
   the command line.  The required-opts argument should be a sequence in which
   each element is either a keyword that corresponds to a command-line option
   or a sequence of keywords that correspond to a set of mutually exclusive
   command-line options."
  [opts help-str required-opts]
  (when (:help opts)
    (println help-str)
    (System/exit 0))
  (dorun (map #(cond
                (keyword? %)    (validate-single-opt opts help-str %)
                (= (count %) 1) (validate-single-opt opts help-str (first %))
                :else           (validate-multiple-opts opts help-str %))
              required-opts)))

(defn read-zkhosts
  "Obtains the Zookeeper connection settings from zkhosts.properties."
  [zkhosts-path]
  (try
    (let [zkhosts-props (ccprops/read-properties zkhosts-path)
          zkhosts       (.getProperty zkhosts-props "zookeeper")]
      (when (nil? zkhosts)
        (throw
         (Exception. (str "no zookeeper information found in " zkhosts-path))))
      zkhosts)))

(defn no-zk-info
  [help-str]
  (println "either --host or --zkhosts-path is required")
  (println help-str)
  (System/exit 1))

(defn get-zk-connection-str
  [{:keys [host port zkhosts-path]} help-str]
  (cond
   (not (nil? host))         (zk/build-connection-str host port)
   (not (nil? zkhosts-path)) (read-zkhosts zkhosts-path)
   :else                     (no-zk-info help-str)))

(defn handle-hosts
  "Performs tasks for the hosts subcommand."
  [args-vec]
  (let [[opts args help-str] (parse-hosts-args args-vec)]
    (validate-opts opts help-str required-hosts-args)
    (let [acl-file  (get-regular-file opts help-str :acl)
          acl-props (ccprops/read-properties acl-file)
          zk-conn   (get-zk-connection-str opts help-str)]

      (when-not (loader/can-run? acl-props)
        (println "This machine isn't listed as an admin machine in " (:acl opts))
        (System/exit 1))

      (println "Starting to load hosts.")
      (loader/load-hosts zk-conn acl-props)
      (println "Done loading hosts.")
      (System/exit 0))))

(defn handle-files
  "Performs tasks for the files subcommand."
  [args-vec]
  (let [[opts args help-str] (parse-files-args args-vec)]
    (validate-opts opts help-str required-files-args)
    (let [envs-file    (get-regular-file opts help-str :envs-file)
          template-dir (get-directory opts help-str :template-dir)
          envs         (env/load-envs envs-file)
          templates    (if (empty? args) (ct/list-templates template-dir) args)
          dep          (:deployment opts)
          env-name     (or (:env opts) (env/env-for-dep envs dep))
          app          (:app opts)
          env          (env/env-configs envs env-name dep)
          env-path     (str app "." env-name "." dep)
          dest         (:dest opts)]

      (when (nil? env)
        (println "no environment defined for" env-path)
        (System/exit 1))

      (when-not (ft/dir? dest)
        (.mkdirs (file dest)))

      (gen/generate-all-files env template-dir templates dest))))

(defn handle-properties
  "Performs tasks for the props subcommand."
  [args-vec]
  (let [[opts args help-str] (parse-args args-vec)]
    (validate-opts opts help-str required-args)
    (let [envs-file    (get-regular-file opts help-str :envs-file)
          template-dir (get-directory opts help-str :template-dir)
          envs         (env/load-envs envs-file)
          templates    (if (empty? args) (ct/list-templates template-dir) args)
          acl-file     (get-regular-file opts help-str :acl)
          acl-props    (ccprops/read-properties acl-file)
          dep          (:deployment opts)
          env-name     (or (:env opts) (env/env-for-dep envs dep))
          app          (:app opts)
          env          (env/env-configs envs env-name dep)
          env-path     (str app "." env-name "." dep)
          zk-conn      (get-zk-connection-str opts help-str)]

      (when (nil? env)
        (println "no environment defined for" env-path)
        (System/exit 1))

      (when-not (loader/can-run? acl-props)
        (println "This machine isn't listed as an admin machine in " acl-file)
        (System/exit 1))

      (println "Starting to load data into the" env-path "environment...")
      (let [acls (loader/load-acls app env-name dep acl-props)]
        (loader/load-settings zk-conn app env-name dep template-dir templates
                              acls env))
      (println "Done loading data into the" env-path "environment."))))

(defn- get-service-host
  "Obtains the IP address for the host that the service is running on."
  [host]
  (if (nil? host)
    (java.net.InetAddress/getLocalHost)
    (try
      (java.net.InetAddress/getByName host)
      (catch java.net.UnknownHostException e
        (println "host" host "is unknown - please check for typos")
        (System/exit 1)))))

(defn- get-deployment
  "Obtains the deployment for an IP address."
  [host]
  (let [host-ip    (.getHostAddress host)
        deployment (zk/deployment host-ip)]
    (when (nil? deployment)
      (println "no deployment is defined for" host-ip)
      (System/exit 1))
    deployment))

(defn- show-props
  "Shows properties in columns."
  [props]
  (let [prop-name-width (apply max (map count (keys props)))
        fmt             (str "%-" prop-name-width "s = %s\n")]
    (dorun (map #(.printf System/out fmt (to-array %)) props))))

(defn handle-get-props
  "Performs tasks for the get-props subcommand."
  [args-vec]
  (let [[opts prop-names help-str] (parse-get-props-args args-vec)]
    (validate-opts opts help-str required-get-props-args)
    (zk/with-zk (get-zk-connection-str opts help-str)
      (let [service      (:service opts)
            service-host (get-service-host (:service-host opts))
            deployment   (get-deployment service-host)]
        (if (= (count prop-names) 1)
          (println (gp/get-prop deployment service (first prop-names)))
          (show-props (gp/get-props deployment service prop-names)))))))

(defn handle-environments
  "Performs tasks for the envs subcommand."
  [args-vec]
  (let [[opts args help-str] (parse-envs-args args-vec)]
    (validate-opts opts help-str required-envs-args)
    (let [envs-file (get-regular-file opts help-str :envs-file)]
      (cond (:list opts)     (env/list-envs envs-file)
            (:validate opts) (env/validate-envs envs-file)))))

(defn do-template-validation
  "Validates all of the templates in the specified template directory.  If the
   path to an environment definition file is also provided then the templates
   will also be validated against every defined environment to ensure that there
   are no unused or undefined properties."
  [template-dir envs]
  (let [valid? (ct/validate-templates template-dir)
        valid? (and valid? (ct/validate-placeholders template-dir envs))]
    (if valid?
      (println "All templates are valid.")
      (println "Errors were found."))))

(defn handle-templates
  "Performs tasks for the templates subcommand."
  [args-vec]
  (let [[opts args help-str] (parse-templates-args args-vec)]
    (validate-opts opts help-str required-templates-args)
    (let [template-dir (get-directory opts help-str :template-dir)
          envs-file    (get-regular-file opts help-str :envs-file)
          envs         (when-not (nil? envs-file) (env/load-envs envs-file))]
      (cond (:list opts)     (ct/display-template-list template-dir)
            (:validate opts) (do-template-validation template-dir envs)))))

(def ^:private subcommand-fns
  {"help"      (fn [args] (main-help args) (System/exit 0))
   "files"     handle-files
   "props"     handle-properties
   "get-props" handle-get-props
   "hosts"     handle-hosts
   "envs"      handle-environments
   "templates" handle-templates})

(defn main-help
  [args]
  (let [known-cmds (string/join "|" (sort (keys subcommand-fns)))]
    (println "clavin" known-cmds "[options]")
    (println "Each command has its own --help.")))

(defn -main
  [& args-vec]
  (let [cmd      (first args-vec)
        args-vec (vec (drop 1 args-vec))]
    (if (contains? subcommand-fns cmd)
      ((subcommand-fns cmd) args-vec)
      (do
        (println "Something weird happened.")
        (main-help args-vec)
        (System/exit 1)))))

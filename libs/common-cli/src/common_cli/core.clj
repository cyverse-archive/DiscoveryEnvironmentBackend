(ns common-cli.core
  (:requires [clojure.tools.cli :as cli]))

(defn cli-options
  []
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Ports must be 0-65536"]]
   ["-c" "--config PATH" "Path to the config file"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn usage
  "Returns a usage string. desc is the description of the app, util-name is the
   name of the utility, and summary should be the command-line summary returned
   by tools.cli/parse-opts."
  [desc util-name summary]
  (->> [desc
        ""
        (str "Usage:" util-name "[options]")
        ""
        "Options:"
        summary]
       (string/join \newline)))

(defn error-msg
  "Returns the errors as a string."
  [errors]
  (str "Errors:\n\n" (string/join \newline errors)))

(defn exit
  "Exits the program with status, printing off the message first."
  [status message]
  (println message)
  (System/exit status))

(defn handle-args
  "Parses the arguments passed in and handles common functionality like --help
   and --version. Takes a map in the following format:
      {:desc        Utility description
       :util-name   The name of the Utility
       :group-id    The maven/leiningen group ID
       :art-id      The maven/leiningen artifact ID
       :args        The unparsed args
       :cli-options Function that returns the CLI definition}
   All of them are strings except for cli-options and args.
   Returns the map parsed out by tools.cli/parse-opts"
  [{:keys [desc util-name group-id art-id args cli-options]}]
  (let [{:keys [options arguments errors summary] :as s} (cli/parse-opts args (cli-options))]
    (cond
     (:help options)
     (exit 0 (usage desc util-name summary))

     (:version options)
     (exit 0 (props/version-info  group-id art-id))

     errors
     (exit 1 (error-msg errors)))
    s))



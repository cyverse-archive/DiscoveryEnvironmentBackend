(ns riak-migrator.core
  (:use [riak-migrator.saved-searches])
  (:require [common-cli.version :as version]
            [common-cli.core :as ccli]
            [clojure.tools.cli :as cli]
            [clojure.string :as string]))

(def commands
  #{"saved-searches"
     "tree-urls"
     "user-preferences"
     "user-sessions"})

(def base-options
  [["-r" "--riak-host HOST" "The Riak hostname to connect to."]

   ["-o" "--riak-port PORT" "The Riak port to use"]

   ["-v" "--version"]

   ["-h" "--help"]])

(def saved-search-options
  (concat
   base-options
   [["-s" "--service-host HOST" "The hostname of the saved-search service"]
    ["-p" "--service-port PORT" "The port for the saved-search service"]]))

(def command-options
  {"saved-searches" saved-search-options})

(defmulti command
  (fn [cmd options]
      cmd))

(defmethod command "saved-searches"
  [cmd options]
  (saved-searches options))

(def app-info
  {:desc "DE tool for migrating data from Riak to PostgreSQL"
   :app-name "riak-migrator"
   :group-id "org.iplantc"
   :art-id "riak-migrator"})

(defn -main
  [& args]
  (when-not (contains? commands (first args))
    (println "Command must be one of: " commands)
    (println "Each command has its own --help option.")
    (System/exit 1))
  (let [cmd (first args)
        cmd-args (rest args)
        cmd-opts (get command-options cmd)
        {:keys [desc app-name group-id art-id]}    app-info
        {:keys [options arguments errors summary]} (cli/parse-opts cmd-args cmd-opts)]
     (cond
      (:help options)
      (ccli/exit 0 (ccli/usage desc app-name summary))

      (:version options)
      (ccli/exit 0 (version/version-info group-id art-id))

      errors
      (ccli/exit 1 (ccli/error-msg errors))

      (not (:riak-host options))
      (ccli/exit 1 "You must specify a --riak-host.")

      (when-not (:riak-port options))
      (ccli/exit 1 "You must specify a --riak-port"))
     (command cmd options)))

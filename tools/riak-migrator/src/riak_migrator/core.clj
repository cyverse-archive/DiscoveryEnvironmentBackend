(ns riak-migrator.core
  (:gen-class)
  (:use [riak-migrator.saved-searches]
        [riak-migrator.tree-urls]
        [riak-migrator.user-sessions]
        [riak-migrator.user-preferences])
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

   ["-o" "--riak-port PORT" "The Riak port to use"
    :default "31301"]

   ["-d" "--db-host HOST" "The hostname for the DE database"]

   ["-b" "--db-port PORT" "The port for the DE database"
    :default "5432"]

   ["-u" "--db-user USER" "The username for the DE datbase"
    :default "de"]

   ["-n" "--db-name DB" "The name of the DE database"
    :default "de"]

   ["-i" "--icat-host HOST" "The hostname for the ICAT database"]

   ["-c" "--icat-port PORT" "The port for the ICAT database"
    :default "5432"]

   ["-a" "--icat-user USER" "The username for the ICAT database"]

   ["-t" "--icat-name DB" "The name of the ICAT database"
    :default "ICAT"]

   ["-s" "--service-host HOST" "The hostname of the saved-search service"]

   ["-p" "--service-port PORT" "The port for the saved-search service"]

   ["-v" "--version"]

   ["-h" "--help"]])

(defn command
  [cmd options]
  (case cmd
    "saved-searches"   (saved-searches options)
    "tree-urls"        (tree-urls options)
    "user-sessions"    (user-sessions options)
    "user-preferences" (user-preferences options)
    (do (println "Unknown command:" cmd)
      (System/exit 1))))

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
  (let [cmd      (first args)
        cmd-args (rest args)
        {:keys [desc app-name group-id art-id]}    app-info
        {:keys [options arguments errors summary]} (cli/parse-opts cmd-args base-options)]
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

(ns jex.config
  (:require [bouncer [core :as b] [validators :as v]]
            [common-cfg.cfg :as cfg]))

(dosync
 (ref-set
  cfg/validators
  {:jar-path        [v/required cfg/stringv]
   :porklock-tag    [v/required cfg/stringv]
   :nfs-base        [v/required cfg/stringv]
   :irods-base      [v/required cfg/stringv]
   :irods-user      [v/required cfg/stringv]
   :irods-pass      [v/required cfg/stringv]
   :irods-host      [v/required cfg/stringv]
   :irods-port      [v/required cfg/stringv]
   :irods-zone      [v/required cfg/stringv]
   :irods-resc      cfg/stringv
   :icommands-path  [v/required cfg/stringv]
   :condor-log-path [v/required cfg/stringv]
   :listen-port     [v/required cfg/stringv]
   :filter-files    [v/required cfg/stringv]
   :run-on-nfs      [v/required cfg/stringv]
   :condor-config   [v/required cfg/stringv]
   :path            [v/required cfg/stringv]
   :request-disk    [v/required cfg/stringv]
   :jex-events      [v/required cfg/stringv]
   :log-file        cfg/stringv
   :log-size        cfg/stringv
   :log-backlog     cfg/stringv
   :log-level       cfg/stringv})

 (ref-set
  cfg/filters
  #{:irods-password}))

(defn jar-path
  "Returns the path to porklock on the filesystem out on the Condor cluster."
  []
  (:jar-path @cfg/cfg))

(defn porklock-tag
  "Returns the docker tag that should be used when invoking the porklock container."
  []
  (:porklock-tag @cfg/cfg))

(defn nfs-base
  "Returns the path to the NFS directory on the submission host."
  []
  (:nfs-base @cfg/cfg))

(defn irods-base
  "Returns the path to the home directory in iRODS. Usually /iplant/home"
  []
  (:irods-base @cfg/cfg))

(defn irods-user
  "Returns the user that porklock should connect as."
  []
  (:irods-user @cfg/cfg))

(defn irods-pass
  "Returns the iRODS user's password."
  []
  (:irods-pass @cfg/cfg))

(defn irods-host
  "Returns the iRODS hostname/IP address."
  []
  (:irods-host @cfg/cfg))

(defn irods-port
  "Returns the iRODS port."
  []
  (:irods-port @cfg/cfg))

(defn irods-zone
  "Returns the iRODS zone."
  []
  (:irods-zone @cfg/cfg))

(defn irods-resc
  "Returns the iRODS resource."
  []
  (:irods-resc @cfg/cfg))

(defn icommands-path
  "Returns the path to the iRODS icommands out on the Condor cluster."
  []
  (:icommands-path @cfg/cfg))

(defn condor-log-path
  "Returns the path to the logs directory for Condor on the submission host."
  []
  (:condor-log-path @cfg/cfg))

(defn listen-port
  "Returns the port to accept requests on as an integer."
  []
  (Integer/parseInt (:listen-port @cfg/cfg)))

(defn filter-files
  "A vector of filenames that should not be returned by porklock."
  []
  (:filter-files @cfg/cfg))

(defn run-on-nfs
  "Whether or not the JEX should run on NFS as a boolean."
  []
  (:run-on-nfs @cfg/cfg))

(defn condor-config
  "The path to the condor_config file."
  []
  (:condor-config @cfg/cfg))

(defn path-env
  "The PATH environment variable value."
  []
  (:path @cfg/cfg))

(defn request-disk
  "The amount of disk space needed for a job, in kilobytes."
  []
  (:request-disk @cfg/cfg))

(defn jex-events-url
  "The URL to the jobs endpoint of the jex-events service"
  []
  (:jex-events @cfg/cfg))

(defn log-level
  "The log level. One of log, trace, debug, info, warn, error, or fatal."
  []
  (keyword (:log-level @cfg/cfg)))

(defn log-file
  "The path to the log file."
  []
  (:log-file @cfg/cfg))

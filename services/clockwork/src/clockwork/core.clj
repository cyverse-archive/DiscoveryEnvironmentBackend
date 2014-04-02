(ns clockwork.core
  (:gen-class)
  (:use [slingshot.slingshot :only [try+]])
  (:require [clojure.tools.logging :as log]
            [clojure.tools.cli :as cli]
            [clojure.string :as string]
            [clojure-commons.error-codes :as ce]
            [clockwork.config :as config]
            [clockwork.notifications :as cn]
            [clockwork.tree-urls :as ctu]
            [clojurewerkz.quartzite.jobs :as qj]
            [clojurewerkz.quartzite.schedule.cron :as qsc]
            [clojurewerkz.quartzite.schedule.simple :as qss]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as qt]))

(defn- split-timestamp
  "Splits a timestamp into its components.  The timestamp should be in the format, HH:MM.  If
   seconds are included in the timestamp, they will be ignored."
  [timestamp error-message]
  (try+
   (->> (string/split timestamp #":")
        (map #(Long/parseLong %))
        (take 2))
   (catch NumberFormatException e
     (log/error error-message timestamp)
     (System/exit 1))))

(defn- tree-urls-cleanup-start
  "The start time for the tree URLs cleanup."
  []
  (split-timestamp
   (config/tree-urls-cleanup-start)
   "Invalid tree URLs cleanup start time:"))

(defn- notification-cleanup-start
  "The start time for the notification cleanup."
  []
  (split-timestamp
   (config/notification-cleanup-start)
   "Invalid notification cleanup start time:"))

(defn- qualified-name
  "Creates a qualified name for a prefix and a given basename."
  [prefix base]
  (str prefix \. base))

(def ^:private job-name (partial qualified-name "jobs"))
(def ^:private trigger-name (partial qualified-name "triggers"))

(qj/defjob clean-up-old-tree-urls
  [ctx]
  (ctu/clean-up-old-tree-urls))

(defn- schedule-clean-up-old-tree-urls-job
  "Schedules the job to remove old tree URLs from the external storage."
  ([hr min]
     (let [basename "tree-urls.1"
           job      (qj/build
                     (qj/of-type clean-up-old-tree-urls)
                     (qj/with-identity (qj/key (job-name basename))))
           trigger  (qt/build
                     (qt/with-identity (qt/key (trigger-name basename)))
                     (qt/with-schedule (qsc/schedule
                                        (qsc/daily-at-hour-and-minute hr min)
                                        (qsc/ignore-misfires))))]
       (qs/schedule job trigger)
       (log/debug (qs/get-trigger (trigger-name basename)))))
  ([]
     (apply schedule-clean-up-old-tree-urls-job (tree-urls-cleanup-start))))

(qj/defjob clean-up-old-notifications
  [ctx]
  (cn/clean-up-old-notifications))

(defn- schedule-notification-cleanup-job
  "Schedules the job to publish notification cleanup tasks."
  ([hr min]
     (let [basename "notification-cleanup.1"
           job      (qj/build
                     (qj/of-type clean-up-old-notifications)
                     (qj/with-identity (qj/key (job-name basename))))
           trigger  (qt/build
                     (qt/with-identity (qt/key (trigger-name basename)))
                     (qt/with-schedule (qsc/schedule
                                        (qsc/daily-at-hour-and-minute hr min)
                                        (qsc/ignore-misfires))))]
       (qs/schedule job trigger)
       (log/debug (qs/get-trigger (trigger-name basename)))))
  ([]
     (apply schedule-notification-cleanup-job (notification-cleanup-start))))

(defn- init-scheduler
  "Initializes the scheduler."
  []
  (qs/initialize)
  (qs/start)
  (when (config/tree-urls-cleanup-enabled)
    (schedule-clean-up-old-tree-urls-job))
  (when (config/notification-cleanup-enabled)
    (schedule-notification-cleanup-job)))

(def cli-options
  [["-l" "--local-config" "use a local configuraiton file"]
   ["-h" "--help" "display the help message"]
   ["-c" "--config PATH" "Path to the config file"]])

(defn usage
  [summary]
  (->> ["Scheduled jobs for the iPlant Discovery Environment."
        ""
        "Usage: clockwork [options]"
        ""
        "Options:"
        summary]
       (string/join \newline)))

(defn error-msg
  [errors]
  (str "Errors:\n\n" (string/join \newline errors)))

(defn exit
  [status message]
  (println message)
  (System/exit status))

(defn configurate
  [options]
  (cond
   (:config options)
   (config/load-config-from-file (:config options))

   (:local-config options)
   (config/load-config-from-file)

   :else
   (config/load-config-from-zookeeper)))

(defn -main
  "Initializes the Quartzite scheduler and schedules jobs."
  [& args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)]
    (cond
     (:help options)
     (exit 0 (usage summary))

     errors
     (exit 1 (error-msg errors)))
    (configurate options)
    (log/info "clockwork startup")
    (init-scheduler)))

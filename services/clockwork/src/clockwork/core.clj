(ns clockwork.core
  (:gen-class)
  (:use [slingshot.slingshot :only [try+]])
  (:require [clojure.tools.logging :as log]
            [clojure.tools.cli :as cli]
            [clojure.string :as string]
            [clojure-commons.error-codes :as ce]
            [clockwork.config :as config]
            [clockwork.notifications :as cn]
            [clojurewerkz.quartzite.jobs :as qj]
            [clojurewerkz.quartzite.schedule.cron :as qsc]
            [clojurewerkz.quartzite.schedule.simple :as qss]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as qt]
            [common-cli.core :as ccli]
            [me.raynes.fs :as fs]
            [service-logging.thread-context :as tc]))

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
  (when (config/notification-cleanup-enabled)
    (schedule-notification-cleanup-job)))

(def svc-info
  {:desc "Scheduled jobs for the iPlant Discovery Environment"
   :app-name "clockwork"
   :group-id "org.iplantc"
   :art-id "clockwork"
   :service "clockwork"})

(defn cli-options
  []
  [["-c" "--config PATH" "Path to the config file"
    :default "/etc/iplant/de/clockwork.properties"]
   ["-v" "--version" "Print out the version number."]
   ["-h" "--help"]])

(defn -main
  [& args]
  (tc/with-logging-context svc-info
    (let [{:keys [options arguments errors summary]} (ccli/handle-args svc-info args cli-options)]
      (when-not (fs/exists? (:config options))
        (ccli/exit 1 (str "The config file does not exist.")))
      (when-not (fs/readable? (:config options))
        (ccli/exit 1 "The config file is not readable."))
      (log/info "clockwork startup")
      (config/load-config-from-file (:config options))
      (init-scheduler))))

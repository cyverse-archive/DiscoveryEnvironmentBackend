(ns metadactyl.tasks
  (:require [metadactyl.service.apps :as apps]
            [metadactyl.util.config :as config])
  (:import [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

(def ^:private thread-pool-size 10)
(def ^:private executor (ScheduledThreadPoolExecutor. thread-pool-size))

(defn set-logging-context!
  "Sets the logging ThreadContext for the threads in the task thread pool."
  [cm]
  (apps/set-logging-context! cm))

(defn- schedule-task
  "Schedules a single periodic task."
  [f delay interval]
  (.scheduleAtFixedRate executor f delay interval TimeUnit/MINUTES))

(defn schedule-tasks
  "Schedules periodic tasks."
  []
  (schedule-task apps/sync-job-statuses 0 (config/job-status-poll-interval)))

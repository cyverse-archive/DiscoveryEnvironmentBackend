(ns donkey.tasks
  (:require [donkey.services.metadata.apps :as apps]
            [donkey.util.config :as config])
  (:import [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

(def ^:private thread-pool-size 10)
(def ^:private executor (ScheduledThreadPoolExecutor. thread-pool-size))

(defn- schedule-task
  "Schedules a single periodic task."
  [f delay interval]
  (.scheduleAtFixedRate executor f delay interval TimeUnit/MINUTES))

(defn schedule-tasks
  "Schedules any periodic tasks required by Donkey."
  []
  (schedule-task apps/sync-job-statuses 0 (config/donkey-job-status-poll-interval)))

(ns donkey.services.metadata.util)

(def failed-status "Failed")
(def completed-status "Completed")
(def submitted-status "Submitted")
(def idle-status "Idle")
(def running-status "Running")
(def completed-status-codes #{failed-status completed-status})

(defn is-completed?
  [job-status]
  (completed-status-codes job-status))

(def not-completed? (complement is-completed?))

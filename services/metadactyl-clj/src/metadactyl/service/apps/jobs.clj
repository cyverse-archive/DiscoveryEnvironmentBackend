(ns metadactyl.service.apps.jobs
  (:require [metadactyl.util.json :as json-util]))

(defn submit
  [apps-client submission]
  (->> (.prepareJobSubmission apps-client submission)
       (json-util/log-json "job")
       (.sendJobSubmission apps-client submission)))

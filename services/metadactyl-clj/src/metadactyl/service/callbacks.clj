(ns metadactyl.service.callbacks
  (:require [clojure.tools.logging :as log]
            [metadactyl.persistence.jobs :as jp]
            [metadactyl.service.apps :as apps]
            [metadactyl.util.service :as service]))

(defn update-de-job-status
  [{end-date :completion_date :keys [status uuid] {:keys [state]}}]
  (service/assert-valid uuid "no job UUID provided")
  (service/assert-valid status "no status provided")
  (log/info (str "received a status update for DE job " uuid ": status = " status))
  (when-not (= status jp/submitted-status)
    (apps/update-job-status uuid status end-date)))

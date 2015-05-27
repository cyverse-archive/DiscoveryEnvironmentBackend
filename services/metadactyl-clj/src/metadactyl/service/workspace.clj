(ns metadactyl.service.workspace
  (:require [metadactyl.persistence.workspace :as wp]
            [metadactyl.util.service :as service]))

(defn get-workspace
  [{:keys [username]}]
  (service/assert-found (wp/get-workspace username) "workspace for" username))

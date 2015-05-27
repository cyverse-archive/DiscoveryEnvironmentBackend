(ns metadactyl.service.workspace
  (:use [korma.db :only [transaction]])
  (:require [metadactyl.persistence.workspace :as wp]
            [metadactyl.util.service :as service]))

(defn get-workspace
  [{:keys [username]}]
  (transaction (service/assert-found (wp/get-workspace username) "workspace for" username)))

(defn create-workspace
  [{:keys [username]}]
  (transaction
   (service/assert-not-found (wp/get-workspace username) "workspace for" username)
   (wp/create-workspace username)))

(ns metadactyl.service.workspace
  (:use [korma.db :only [transaction]])
  (:require [metadactyl.persistence.workspace :as wp]))

(defn get-workspace
  [{:keys [username]}]
  (if-let [workspace (wp/get-workspace username)]
    (assoc workspace :new_workspace false)
    (assoc (wp/create-workspace username) :new_workspace true)))

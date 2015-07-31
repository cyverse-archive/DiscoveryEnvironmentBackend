(ns fishy.service.groups
  (:require [fishy.clients.grouper :as grouper]
            [fishy.service.format :as fmt]
            [fishy.util.service :as service]))

(defn group-search
  [{:keys [user search folder]}]
  {:groups (mapv fmt/format-group (grouper/group-search user folder search))})

(defn get-group
  [group-id {:keys [user]}]
  (if-let [group (grouper/get-group user group-id)]
    (fmt/format-group-with-detail group)
    (service/not-found "group" group-id)))

(defn get-group-members
  [group-id {:keys [user]}]
  {:members (mapv fmt/format-subject (grouper/get-group-members user group-id))})

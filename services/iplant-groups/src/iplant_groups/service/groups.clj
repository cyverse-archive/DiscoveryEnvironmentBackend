(ns iplant_groups.service.groups
  (:require [iplant_groups.clients.grouper :as grouper]
            [iplant_groups.service.format :as fmt]
            [iplant_groups.util.service :as service]))

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
  (let [[subjects attribute-names] (grouper/get-group-members user group-id)]
    {:members (mapv #(fmt/format-subject attribute-names %) subjects)}))

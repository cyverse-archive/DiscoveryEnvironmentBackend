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

(ns fishy.service.groups
  (:require [fishy.clients.grouper :as grouper]
            [fishy.service.format :as fmt]))

(defn group-search
  [{:keys [user search folder]}]
  {:groups (mapv fmt/format-group (grouper/group-search user folder search))})

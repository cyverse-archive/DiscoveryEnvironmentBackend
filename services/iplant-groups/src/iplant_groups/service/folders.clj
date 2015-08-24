(ns iplant_groups.service.folders
  (:require [iplant_groups.clients.grouper :as grouper]
            [iplant_groups.service.format :as fmt]))

(defn folder-search
  [{:keys [user search]}]
  {:folders (mapv fmt/format-folder (grouper/folder-search user search))})

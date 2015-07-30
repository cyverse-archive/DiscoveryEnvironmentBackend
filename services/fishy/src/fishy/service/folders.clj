(ns fishy.service.folders
  (:require [fishy.clients.grouper :as grouper]
            [fishy.service.format :as fmt]))

(defn folder-search
  [{:keys [user search]}]
  {:folders (mapv fmt/format-folder (grouper/folder-search user search))})

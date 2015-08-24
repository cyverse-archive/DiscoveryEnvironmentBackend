(ns iplant_groups.service.subjects
  (:require [iplant_groups.clients.grouper :as grouper]
            [iplant_groups.service.format :as fmt]))

(defn subject-search
  [{:keys [user search]}]
  {:subjects (mapv fmt/format-subject (grouper/subject-search user search))})

(defn get-subject
  [subject-id {:keys [user]}]
  (fmt/format-subject (grouper/get-subject user subject-id)))

(defn groups-for-subject
  [subject-id {:keys [user]}]
  {:groups (mapv fmt/format-group (grouper/groups-for-subject user subject-id))})

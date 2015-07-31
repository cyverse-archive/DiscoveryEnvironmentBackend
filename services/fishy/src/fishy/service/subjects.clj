(ns fishy.service.subjects
  (:require [fishy.clients.grouper :as grouper]
            [fishy.service.format :as fmt]))

(defn subject-search
  [{:keys [user search]}]
  {:subjects (mapv fmt/format-subject (grouper/subject-search user search))})

(defn get-subject
  [subject-id {:keys [user]}]
  (fmt/format-subject (grouper/get-subject user subject-id)))

(defn groups-for-subject
  [subject-id {:keys [user]}]
  {:groups (mapv fmt/format-group (grouper/groups-for-subject user subject-id))})

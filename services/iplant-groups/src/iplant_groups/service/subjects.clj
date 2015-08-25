(ns iplant_groups.service.subjects
  (:require [iplant_groups.clients.grouper :as grouper]
            [iplant_groups.service.format :as fmt]))

(defn subject-search
  [{:keys [user search]}]
  (let [[subjects attribute-names] (grouper/subject-search user search)]
    {:subjects (mapv #(fmt/format-subject attribute-names %) subjects)}))

(defn get-subject
  [subject-id {:keys [user]}]
  (let [[subject attribute-names] (grouper/get-subject user subject-id)]
    (fmt/format-subject attribute-names subject)))

(defn groups-for-subject
  [subject-id {:keys [user]}]
  {:groups (mapv fmt/format-group (grouper/groups-for-subject user subject-id))})

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

(defn get-group-privileges
  [group-id {:keys [user]}]
  (let [[privileges attribute-names] (grouper/get-group-privileges user group-id)]
    {:privileges (mapv #(fmt/format-privilege attribute-names %) privileges)}))

(defn add-group
  [{:keys [type name description display_extension]} {:keys [user]}]
  (let [group (grouper/add-group user type name display_extension description)]
    (fmt/format-group-with-detail group)))

(defn add-group-privilege
  [group-id subject-id privilege-name {:keys [user]}]
  (let [[privilege attribute-names] (grouper/add-group-privileges user group-id subject-id [privilege-name])]
    (fmt/format-privilege attribute-names privilege :wsSubject)))

(defn remove-group-privilege
  [group-id subject-id privilege-name {:keys [user]}]
  (let [[privilege attribute-names] (grouper/remove-group-privileges user group-id subject-id [privilege-name])]
    (fmt/format-privilege attribute-names privilege :wsSubject)))

(defn update-group
  [group-id {:keys [name description display_extension]} {:keys [user]}]
  (let [group (grouper/update-group user group-id name display_extension description)]
    (fmt/format-group-with-detail group)))

(defn delete-group
  [group-id {:keys [user]}]
  (fmt/format-group (grouper/delete-group user group-id)))

(ns iplant_groups.service.folders
  (:require [iplant_groups.clients.grouper :as grouper]
            [iplant_groups.service.format :as fmt]
            [iplant_groups.util.service :as service]))

(defn folder-search
  [{:keys [user search]}]
  {:folders (mapv fmt/format-folder (grouper/folder-search user search))})

(defn get-folder
  [folder-id {:keys [user]}]
  (if-let [folder (grouper/get-folder user folder-id)]
    (fmt/format-folder folder)
    (service/not-found "folder" folder-id)))

(defn get-folder-privileges
  [folder-id {:keys [user]}]
  (let [[privileges attribute-names] (grouper/get-folder-privileges user folder-id)]
    {:privileges (mapv #(fmt/format-privilege attribute-names %) privileges)}))

(defn add-folder
  [{:keys [name description display_extension]} {:keys [user]}]
  (let [folder (grouper/add-folder user name display_extension description)]
    (fmt/format-folder folder)))

(defn add-folder-privilege
  [folder-id subject-id privilege-name {:keys [user]}]
  (let [[privilege attribute-names] (grouper/add-folder-privileges user folder-id subject-id [privilege-name])]
    (fmt/format-privilege attribute-names privilege :wsSubject)))

(defn remove-folder-privilege
  [folder-id subject-id privilege-name {:keys [user]}]
  (let [[privilege attribute-names] (grouper/remove-folder-privileges user folder-id subject-id [privilege-name])]
    (fmt/format-privilege attribute-names privilege :wsSubject)))

(defn update-folder
  [folder-id {:keys [name description display_extension]} {:keys [user]}]
  (let [folder (grouper/update-folder user folder-id name display_extension description)]
    (fmt/format-folder folder)))

(defn delete-folder
  [folder-id {:keys [user]}]
  (fmt/format-folder (grouper/delete-folder user folder-id)))

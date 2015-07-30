(ns fishy.service.folders
  (:use [medley.core :only [remove-vals]])
  (:require [fishy.clients.grouper :as grouper]))

(defn- format-folder
  [folder]
  (->> {:description       (:description folder)
        :display_extension (:displayExtension folder)
        :display_name      (:displayName folder)
        :extension         (:extension folder)
        :id_index          (:idIndex folder)
        :name              (:name folder)
        :id                (:uuid folder)}
       (remove-vals nil?)))

(defn folder-search
  [{:keys [user search]}]
  {:folders (mapv format-folder (grouper/folder-search user search))})

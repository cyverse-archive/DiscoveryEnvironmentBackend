(ns fishy.service.groups
  (:use [medley.core :only [remove-vals]])
  (:require [fishy.clients.grouper :as grouper]))

(defn- format-group
  [group]
  (->> {:description       (:description group)
        :display_extension (:displayExtension group)
        :display_name      (:displayName group)
        :extension         (:extension group)
        :id_index          (:idIndex group)
        :name              (:name group)
        :type              (:typeOfGroup group)
        :id                (:uuid group)}
       (remove-vals nil?)))

(defn group-search
  [{:keys [user search folder]}]
  {:groups (mapv format-group (grouper/group-search user folder search))})

(ns fishy.service.subjects
  (:use [medley.core :only [remove-vals]])
  (:require [fishy.clients.grouper :as grouper]))

(defn- format-subject
  [subject]
  (->> {:attribute_values  (:attributeValues subject)
        :id                (:id subject)
        :name              (:name subject)
        :source_id         (:sourceId subject)}
       (remove-vals nil?)))

(defn subject-search
  [{:keys [user search]}]
  {:subjects (mapv format-subject (grouper/subject-search user search))})

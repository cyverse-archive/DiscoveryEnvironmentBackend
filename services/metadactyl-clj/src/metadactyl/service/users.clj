(ns metadactyl.service.users
  (:use [metadactyl.util.conversions :only [remove-nil-vals]])
  (:require [metadactyl.persistence.users :as up]))

(defn by-id
  [{:keys [ids]}]
  {:users (mapv remove-nil-vals (up/by-id ids))})

(ns metadactyl.service.users
  (:use [metadactyl.util.conversions :only [remove-nil-vals]])
  (:require [kameleon.queries :as kq]
            [metadactyl.persistence.users :as up]))

(defn by-id
  [{:keys [ids]}]
  {:users (mapv remove-nil-vals (up/by-id ids))})

(defn authenticated
  [{:keys [username]}]
  (remove-nil-vals (up/for-username username)))

(defn login
  [{:keys [username]} {:keys [ip-address user-agent]}]
  {:login_time (kq/record-login username ip-address user-agent)})

(defn logout
  [{:keys [username]} {:keys [ip-address login-time]}]
  (kq/record-logout username ip-address login-time)
  nil)

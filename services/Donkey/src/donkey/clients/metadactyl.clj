(ns donkey.clients.metadactyl
  (:require [cheshire.core :as cheshire]
            [donkey.clients.metadactyl.raw :as raw]
            [donkey.util.service :as service]))

(defn get-app
  [app-id]
  (->> (raw/get-app app-id)
       (:body)
       (service/decode-json)))

(defn get-app-details
  [app-id]
  (->> (raw/get-app-details app-id)
       (:body)
       (service/decode-json)))

(defn admin-list-tool-requests
  [params]
  (->> (raw/admin-list-tool-requests params)
       (:body)
       (service/decode-json)))

(defn list-tool-request-status-codes
  [params]
  (-> (raw/list-tool-request-status-codes params)
      (:body)
      (service/decode-json)))

(defn get-tools-in-app
  [app-id]
  (-> (raw/get-tools-in-app app-id)
      (:body)
      (service/decode-json)))

(defn import-tools
  [body]
  (raw/import-tools (cheshire/encode body)))

(defn submit-job
  [submission]
  (raw/submit-job (cheshire/encode submission)))

(defn get-workspace
  []
  (-> (raw/get-workspace)
      (:body)
      (service/decode-json)))

(defn get-users-by-id
  [ids]
  (-> (raw/get-users-by-id (cheshire/encode {:ids ids}))
      (:body)
      (service/decode-json)))

(defn get-authenticated-user
  []
  (-> (raw/get-authenticated-user)
      (:body)
      (service/decode-json)))

(defn record-login
  [ip-address user-agent]
  (-> (raw/record-login ip-address user-agent)
      (:body)
      (service/decode-json)))

(defn record-logout
  [ip-address login-time]
  (raw/record-logout ip-address login-time)
  nil)

(ns donkey.clients.notifications.raw
  (:use [donkey.util.transformers :only [secured-params]])
  (:require [cemerick.url :as curl]
            [clj-http.client :as client]
            [donkey.util.config :as config]))

(def na-sort-params [:limit :offset :sortfield :sortdir])
(def na-filter-params [:seen :filter])
(def na-message-params (concat na-sort-params na-filter-params))
(def na-system-message-params [:active-only :type :limit :offset])

(defn- na-url
  [& components]
  (str (apply curl/url (config/notificationagent-base) components)))

(defn get-messages
  [params]
  (client/get (na-url "messages")
              {:query-params     (secured-params params na-message-params)
               :as               :stream
               :follow-redirects false}))

(defn get-unseen-messages
  [params]
  (client/get (na-url "unseen-messages")
              {:query-params     (secured-params params na-message-params)
               :as               :stream
               :follow-redirects false}))

(defn last-ten-messages
  []
  (client/get (na-url "last-ten-messages")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn count-messages
  [params]
  (client/get (na-url "count-messages")
              {:query-params     (secured-params params na-filter-params)
               :as               :stream
               :follow-redirects false}))

(defn delete-notifications
  [body]
  (client/post (na-url "delete")
               {:query-params     (secured-params)
                :as               :stream
                :content-type     :json
                :body             body
                :follow-redirects false}))

(defn delete-all-notifications
  [params]
  (client/delete (na-url "delete-all")
                 {:query-params     (secured-params params na-message-params)
                  :as               :stream
                  :follow-redirects false}))

(defn mark-notifications-seen
  [body]
  (client/post (na-url "seen")
               {:query-params     (secured-params)
                :as               :stream
                :content-type     :json
                :body             body
                :follow-redirects false}))

(defn mark-all-notifications-seen
  [body]
  (client/post (na-url "mark-all-seen")
               {:query-params     (secured-params)
                :as               :stream
                :content-type     :json
                :body             body
                :follow-redirects false}))

(defn send-notification
  [body]
  (client/post (na-url "notification")
               {:query-params     (secured-params)
                :as               :stream
                :content-type     :json
                :body             body
                :follow-redirects false}))

(defn get-system-messages
  []
  (client/get (na-url "system" "messages")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-new-system-messages
  []
  (client/get (na-url "system" "new-messages")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn get-unseen-system-messages
  []
  (client/get (na-url "system" "unseen-messages")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn mark-system-messages-received
  [body]
  (client/post (na-url "system" "received")
               {:query-params     (secured-params)
                :as               :stream
                :content-type     :json
                :body             body
                :follow-redirects false}))

(defn mark-all-system-messages-received
  [body]
  (client/post (na-url "system" "mark-all-received")
               {:query-params     (secured-params)
                :as               :stream
                :content-type     :json
                :body             body
                :follow-redirects false}))

(defn mark-system-messages-seen
  [body]
  (client/post (na-url "system" "seen")
               {:query-params     (secured-params)
                :as               :stream
                :content-type     :json
                :body             body
                :follow-redirects false}))

(defn mark-all-system-messages-seen
  [body]
  (client/post (na-url "system" "mark-all-seen")
               {:query-params     (secured-params)
                :as               :stream
                :content-type     :json
                :body             body
                :follow-redirects false}))

(defn delete-system-messages
  [body]
  (client/post (na-url "system" "delete")
               {:query-params     (secured-params)
                :as               :stream
                :content-type     :json
                :body             body
                :follow-redirects false}))

(defn delete-all-system-messages
  [params]
  (client/delete (na-url "system" "delete-all")
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

(defn admin-add-system-message
  [body]
  (client/put (na-url "admin" "system")
              {:query-params     (secured-params)
               :as               :stream
               :content-type     :json
               :body             body
               :follow-redirects false}))

(defn admin-list-system-types
  []
  (client/get (na-url "admin" "system-types")
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn admin-list-system-messages
  [params]
  (client/get (na-url "admin" "system")
              {:query-params     (secured-params params na-system-message-params)
               :as               :stream
               :follow-redirects false}))

(defn admin-get-system-message
  [uuid]
  (client/get (na-url "admin" "system" uuid)
              {:query-params     (secured-params)
               :as               :stream
               :follow-redirects false}))

(defn admin-update-system-message
  [uuid body]
  (client/post (na-url "admin" "system" uuid)
               {:query-params     (secured-params)
                :as               :stream
                :content-type     :json
                :body             body
                :follow-redirects false}))

(defn admin-delete-system-message
  [uuid]
  (client/delete (na-url "admin" "system" uuid)
                 {:query-params     (secured-params)
                  :as               :stream
                  :follow-redirects false}))

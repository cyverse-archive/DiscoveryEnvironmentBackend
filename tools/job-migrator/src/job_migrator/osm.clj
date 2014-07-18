(ns job-migrator.osm
  (:require [cemerick.url :as curl]
            [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [job-migrator.config :as config]))

(defn get-job [job-id]
  (-> (str (curl/url (config/osm-base-url) (config/osm-jobs-bucket) "query"))
      (http/post {:body         (cheshire/encode {:state.uuid job-id})
                  :content-type :json
                  :as           :json})
      (:body)
      (:objects)
      (first)
      (:state)))

(defn get-job-submission [job-id]
  (-> (str (curl/url (config/osm-base-url) (config/osm-job-request-bucket) "query"))
      (http/post {:body         (cheshire/encode {:state.jobUuid job-id})
                  :content-type :json
                  :as           :json})
      (:body)
      (:objects)
      (first)
      (:state)
      (:experiment)))

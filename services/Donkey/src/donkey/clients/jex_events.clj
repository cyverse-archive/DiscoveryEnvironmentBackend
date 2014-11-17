(ns donkey.clients.jex-events
  (:require [clj-http.client :as http]
            [cemerick.url :as curl]
            [donkey.util.config :as config]))

(defn- jex-events-url
  [& components]
  (str (apply curl/url (config/jex-events-base-url) components)))

(defn get-job-state
  [job-id]
  (-> (jex-events-url "last-events" job-id)
      (http/get {:as :json})
      (:body)
      (:state)))

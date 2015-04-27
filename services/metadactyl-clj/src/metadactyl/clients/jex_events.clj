(ns metadactyl.clients.jex-events
  (:use [slingshot.slingshot :only [try+]])
  (:require [clj-http.client :as http]
            [cemerick.url :as curl]
            [metadactyl.util.config :as config]))

(defn- jex-events-url
  [& components]
  (str (apply curl/url (config/jex-events-base-url) components)))

(defn- job-exists?
  [job-id]
  (try+
   (-> (jex-events-url "invocations" job-id)
       (http/get {:as :json})
       (:body))
   (catch [:status 404] _ nil)))

(defn get-job-state
  [job-id]
  (when (job-exists? job-id)
    (-> (jex-events-url "last-events" job-id)
        (http/get {:as :json})
        (:body)
        (:state))))

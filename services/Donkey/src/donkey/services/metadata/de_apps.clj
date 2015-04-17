(ns donkey.services.metadata.de-apps
  (:use [clojure-commons.validators :only [validate-map]]
        [donkey.auth.user-attributes :only [current-user]])
  (:require [cemerick.url :as curl]
            [clojure.tools.logging :as log]
            [clojure-commons.file-utils :as ft]
            [donkey.clients.jex-events :as jex-events]
            [donkey.clients.metadactyl :as metadactyl]
            [donkey.clients.notifications :as dn]
            [donkey.persistence.apps :as ap]
            [donkey.persistence.jobs :as jp]
            [donkey.services.metadata.property-values :as property-values]
            [donkey.services.metadata.util :as mu]
            [donkey.util.config :as config]
            [donkey.util.service :as service]
            [kameleon.db :as db])
  (:import [java.util UUID]))

(defn- de-job-callback-url
  []
  (str (curl/url (config/donkey-base-url) "callbacks" "de-job")))

(defn- prepare-submission
  [submission job-id]
  (assoc (mu/update-submission-result-folder submission (ft/build-result-folder-path submission))
    :uuid     (str job-id)
    :callback (de-job-callback-url)))

(defn submit-job-step
  [submission]
  (->> (prepare-submission submission (UUID/randomUUID))
       (metadactyl/submit-job)
       (:id)))

(defn get-job-step-status
  [id]
  (when-let [step (jex-events/get-job-state id)]
    {:status  (:status step)
     :enddate (:completion_date step)}))

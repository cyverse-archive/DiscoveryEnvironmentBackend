(ns metadactyl.service.apps.de.jobs.base
  (:require [metadactyl.metadata.params :as mp]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.service.apps.de.jobs.common :as ca]
            [metadactyl.service.apps.de.jobs.condor]
            [metadactyl.service.apps.de.jobs.fapi]
            [metadactyl.service.apps.de.jobs.protocol]
            [metadactyl.service.apps.de.jobs.util :as util]))

(defn- build-job-request-formatter
  [user submission]
  (let [email    (:email user)
        app-id   (:app_id submission)
        app      (ap/get-app app-id)
        io-maps  (ca/load-io-maps app-id)
        params   (mp/load-app-params app-id)
        defaults (ca/build-default-values-map params)
        params   (group-by :step_id params)]
    (if (util/fapi-app? app)
      (metadactyl.service.apps.de.jobs.fapi.JobRequestFormatter.
       user email submission app io-maps defaults params)
      (metadactyl.service.apps.de.jobs.condor.JobRequestFormatter.
       user email submission app io-maps defaults params))))

(defn build-submission
  [user submission]
  (.buildSubmission (build-job-request-formatter user submission)))

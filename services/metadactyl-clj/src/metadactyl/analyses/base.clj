(ns metadactyl.analyses.base
  (:use [korma.core]
        [metadactyl.util.assertions :only [assert-not-nil]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [metadactyl.analyses.common :as ca]
            [metadactyl.analyses.params :as params]
            [metadactyl.analyses.util :as util]
            [metadactyl.metadata.params :as mp]
            [metadactyl.persistence.app-metadata :as ap]))

(defprotocol JobRequestFormatter
  "A protocol for formatting JEX job requests."
  (buildInputs [_ params])
  (buildOutputs [_ params])
  (buildParams [_ params outputs])
  (buildConfig [_ step])
  (buildEnvironment [_ step])
  (buildComponent [_ step])
  (buildStep [_ step])
  (buildSteps [_])
  (buildSubmission [_]))

(deftype JobRequestFormatterImpl [user email submission app io-maps defaults params
                                  param-formatter]
  JobRequestFormatter

  (buildParams [this params outputs]
    (.buildParams param-formatter submission io-maps outputs defaults params))

  (buildInputs [this params]
    (params/build-inputs (:config submission) params))

  (buildOutputs [this params]
    (params/build-outputs (:config submission) defaults params))

  (buildConfig [this step]
    (let [params-for-step  (params (:id step))
          outputs-for-step (.buildOutputs this params-for-step)
          inputs-for-step  (.buildInputs this params-for-step)
          params-for-step  (.buildParams this params-for-step outputs-for-step)]
      (ca/build-config inputs-for-step outputs-for-step params-for-step)))

  (buildEnvironment [this step]
    (ca/build-environment (:config submission) defaults (params (:id step))))

  (buildComponent [this step]
    (ca/build-component step))

  (buildStep [this step]
    (ca/build-step this step))

  (buildSteps [this]
    (ca/build-steps this app submission))

  (buildSubmission [this]
    (ca/build-submission this user email submission app)))

(defn- param-formatter-for-app
  [app user]
  (if (util/fapi-app? app)
    (metadactyl.analyses.params.FapiParamFormatter. user)
    (metadactyl.analyses.params.DeParamFormatter.)))

(defn- build-job-request-formatter

  [user email submission]
  (let [app-id          (:app_id submission)
        app             (ap/get-app app-id)
        io-maps         (ca/load-io-maps app-id)
        params          (mp/load-app-params app-id)
        defaults        (ca/build-default-values-map params)
        params          (group-by :step_id params)
        param-formatter (param-formatter-for-app app user)]
    (JobRequestFormatterImpl. user email submission app io-maps defaults params
                              param-formatter)))

(defn build-submission
  [user email submission]
  (.buildSubmission (build-job-request-formatter user email submission)))

;; TODO: remove when this is no longer needed.
(defn- load-submission
  [path]
  (-> (slurp path)
      (cheshire.core/decode true)
      (update-in [:app_id] kameleon.uuids/uuidify)))

;; TODO: remove when this is no longer needed.
(defn test-submission
  [user email path]
  (build-submission user email (load-submission path)))

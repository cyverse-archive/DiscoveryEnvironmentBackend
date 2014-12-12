(ns metadactyl.analyses.base
  (:use [korma.core]
        [metadactyl.util.assertions :only [assert-not-nil]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [me.raynes.fs :as fs]
            [metadactyl.analyses.common :as ca]
            [metadactyl.analyses.params :as params]
            [metadactyl.analyses.util :as util]
            [metadactyl.metadata.params :as mp]
            [metadactyl.persistence.app-metadata :as ap]))

(defprotocol JobRequestFormatter
  "A protocol for formatting JEX job requests."
  (buildTreeSelectionArgs [_ param param-value])
  (buildSelectionArgs [_ param param-value])
  (buildFlagArgs [_ param param-value])
  (buildInputArgs [_ param param-value])
  (buildOutputArgs [_ param param-value])
  (buildReferenceGenomeArgs [_ param param-value])
  (buildReferenceSequenceArgs [_ param param-value])
  (buildReferenceAnnotationArgs [_ param param-value])
  (buildGenericArgs [_ param param-value])
  (buildInputs [_ params])
  (buildOutputs [_ params])
  (buildParams [_ params outputs])
  (buildConfig [_ steps step])
  (buildEnvironment [_ step])
  (buildComponent [_ step])
  (buildStep [_ steps step])
  (buildSteps [_])
  (buildSubmission [_]))

;;
;; The job request formatter for DE jobs.
;;
(deftype DeJobRequestFormatter [user email submission app io-maps defaults params]
  JobRequestFormatter

  (buildTreeSelectionArgs [_ param param-value]
    (params/tree-selection-args param param-value))

  (buildSelectionArgs [_ param param-value]
    (params/selection-args param param-value))

  (buildFlagArgs [_ param param-value]
    (params/flag-args param param-value))

  (buildInputArgs [_ param param-value]
    (params/input-args param param-value #(if (string/blank? %) nil (fs/base-name %))))

  (buildOutputArgs [_ param param-value]
    (params/output-args param param-value))

  (buildReferenceGenomeArgs [_ param param-value]
    (params/reference-genome-args param param-value))

  (buildReferenceSequenceArgs [_ param param-value]
    (params/reference-sequence-args param param-value))

  (buildReferenceAnnotationArgs [_ param param-value]
    (params/reference-annotation-args param param-value))

  (buildGenericArgs [_ param param-value]
    (params/generic-args param param-value))

  (buildParams [this params outputs]
    (params/build-params this (:config submission) io-maps outputs defaults params))

  (buildInputs [this params]
    (params/build-inputs submission params))

  (buildOutputs [this params]
    (conj (params/build-outputs (:config submission) defaults params)
          (params/log-output (:archive_logs submission true))))

  (buildConfig [this steps step]
    (let [params-for-step  (params (:id step))
          outputs          (mapcat (comp :output :config) steps)
          outputs-for-step (.buildOutputs this params-for-step)
          stdout           (params/find-redirect-output-filename outputs-for-step "stdout")
          stderr           (params/find-redirect-output-filename outputs-for-step "stderr")
          outputs-for-step (map #(dissoc % :data_source) outputs-for-step)
          inputs-for-step  (.buildInputs this params-for-step)
          params-for-step  (.buildParams this params-for-step (concat outputs outputs-for-step))]
      (assoc (ca/build-config inputs-for-step outputs-for-step params-for-step)
        :stdout stdout
        :stderr stderr)))

  (buildEnvironment [this step]
    (ca/build-environment (:config submission) defaults (params (:id step))))

  (buildComponent [this step]
    (ca/build-component step))

  (buildStep [this steps step]
    (ca/build-step this steps step))

  (buildSteps [this]
    (ca/build-steps this app submission))

  (buildSubmission [this]
    (ca/build-submission this user email submission app)))
;;
;; End of DeJobRequestFormatter
;;

;;
;; The job request formatter for Foundation API jobs.
;;
(deftype FapiJobRequestFormatter [user email submission app io-maps defaults params]
  JobRequestFormatter

  (buildTreeSelectionArgs [_ param param-value]
    (params/tree-selection-args param param-value))

  (buildSelectionArgs [_ param param-value]
    (params/selection-args param param-value))

  (buildFlagArgs [_ param param-value]
    (params/flag-args param param-value))

  (buildInputArgs [_ param param-value]
    (params/input-args param param-value params/remove-irods-home))

  (buildOutputArgs [_ param param-value]
    (params/output-args param param-value))

  (buildReferenceGenomeArgs [_ param param-value]
    (params/reference-genome-args param param-value))

  (buildReferenceSequenceArgs [_ param param-value]
    (params/reference-sequence-args param param-value))

  (buildReferenceAnnotationArgs [_ param param-value]
    (params/reference-annotation-args param param-value))

  (buildGenericArgs [_ param param-value]
    (params/generic-args param param-value))

  (buildParams [this params outputs]
    (concat (params/build-extra-fapi-args user (:name submission) (:output_dir submission))
            (params/build-params this (:config submission) io-maps outputs defaults params)))

  (buildInputs [_ params]
    ;; The inputs array needs to be empty when it's submitted to the JEX, but the batch
    ;; submission code needs it to determine which paramters are inputs. The batch submission
    ;; code will clear out the inputs array before submitting Foundation API jobs to the JEX.
    (params/build-inputs submission params))

  (buildOutputs [_ _]
    [(params/log-output (:archive_logs submission true))])

  (buildConfig [this steps step]
    (let [params-for-step  (params (:id step))
          outputs          (mapcat (comp :output :config) steps)
          outputs-for-step (.buildOutputs this params-for-step)
          inputs-for-step  (.buildInputs this params-for-step)
          params-for-step  (.buildParams this params-for-step (concat outputs outputs-for-step))]
      (ca/build-config inputs-for-step outputs-for-step params-for-step)))

  (buildEnvironment [this step]
    (ca/build-environment (:config submission) defaults (params (:id step))))

  (buildComponent [this step]
    (ca/build-component step))

  (buildStep [this steps step]
    (ca/build-step this steps step))

  (buildSteps [this]
    (ca/build-steps this app submission))

  (buildSubmission [this]
    (ca/build-submission this user email submission app)))
;;
;; End of FapiJobRequestFormatter
;;

(defn- build-job-request-formatter
  [user email submission]
  (let [app-id   (:app_id submission)
        app      (ap/get-app app-id)
        io-maps  (ca/load-io-maps app-id)
        params   (mp/load-app-params app-id)
        defaults (ca/build-default-values-map params)
        params   (group-by :step_id params)]
    (if (util/fapi-app? app)
      (FapiJobRequestFormatter. user email submission app io-maps defaults params)
      (DeJobRequestFormatter. user email submission app io-maps defaults params))))

(defn build-submission
  [user email submission]
  (.buildSubmission (build-job-request-formatter user email submission)))

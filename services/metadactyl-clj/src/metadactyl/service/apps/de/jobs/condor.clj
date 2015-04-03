(ns metadactyl.service.apps.de.jobs.condor
  (:require [clojure.string :as string]
            [me.raynes.fs :as fs]
            [metadactyl.service.apps.de.jobs.common :as ca]
            [metadactyl.service.apps.de.jobs.params :as params]
            [metadactyl.service.apps.de.jobs.protocol]))

(deftype JobRequestFormatter [user email submission app io-maps defaults params]
  metadactyl.service.apps.de.jobs.protocol.JobRequestFormatter

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

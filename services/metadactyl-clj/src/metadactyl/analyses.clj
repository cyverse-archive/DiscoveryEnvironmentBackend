(ns metadactyl.analyses
  (:use [metadactyl.util.conversions :only [remove-nil-vals]]
        [slingshot.slingshot :only [try+]])
  (:require [clojure.tools.logging :as log]
            [metadactyl.persistence.app-metadata :as ap]))

(defn- build-jex-submission
  [app user email submission]
  {:analysis_description (:description app)
   :analysis_id          (:id app)
   :analysis_name        (:name app)
   :callback             (:callback submission)
   :create_output_subdir (:create_output_subdir submission true)
   :description          (:description submission "")
   :email                email
   :execution_target     "condor"
   :name                 (:name submission)
   :notify               (:notify submission)
   :output_dir           (:output_dir submission)
   :request_type         "submit"
   :username             user
   :uuid                 (or (:uuid submission) (UUID/randomUUID))})

(defn submit
  [{:keys [user email]} submission]
  (-> (ap/get-app (:app_id submission))
      (build-jex-submission user email submission)
      (remove-nil-vals)))
